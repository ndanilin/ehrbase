/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School,
 * Jake Smolka (Hannover Medical School),
 * Stefan Spiska (Vitasystems GmbH).

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.service;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.collections4.MapUtils;
import org.apache.xmlbeans.XmlException;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.aql.containment.JsonPathQueryBuilder;
import org.ehrbase.aql.containment.JsonPathQueryResult;
import org.ehrbase.aql.containment.OptJsonPath;
import org.ehrbase.aql.containment.TemplateIdQueryTuple;
import org.ehrbase.configuration.CacheConfiguration;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.ehr.knowledge.TemplateMetaData;
import org.ehrbase.opt.OptVisitor;
import org.ehrbase.opt.query.I_QueryOptMetaData;
import org.ehrbase.opt.query.MapJson;
import org.ehrbase.opt.query.QueryOptMetaData;
import org.openehr.schemas.v1.OBJECTID;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.openehr.schemas.v1.TEMPLATEID;
import org.openehr.schemas.v1.TemplateDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.cache.Cache;
import javax.cache.CacheManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.ehrbase.configuration.CacheConfiguration.OPERATIONAL_TEMPLATE_CACHE;
import static org.ehrbase.configuration.CacheConfiguration.QUERY_CACHE;

/**
 * Look up and caching for archetypes, openEHR showTemplates and Operational Templates. Search in path defined as
 * <ul>
 * <li> 1. System environment ETHERCIS_ARCHETYPE_DIR, ETHERCIS_TEMPLATE_DIR, ETHERCIS_OPT_DIR</li>
 * <li> 2. Application path %USER_HOME%/.ethercis/archetype, %USER_HOME%/.ethercis/template, %USER_HOME%/.ethercis/opt</li>
 * <li> 3. User can also include a source directory by invoking addXYZPath method</li>
 * </ul>
 *
 * <p>
 * The resources extensions are defined by the following default:
 * <ul>
 * <li>ADL: archetype</li>
 * <li>OET: openehr template</li>
 * <li>OPT: operational template</li>
 * </ul>
 * </p>
 *
 * @author C. Chevalley
 */
@Service
public class KnowledgeCacheService implements I_KnowledgeCache, IntrospectService {


    private final Logger log = LoggerFactory.getLogger(this.getClass());


    private final TemplateStorage templateStorage;
    private final Cache<TemplateIdQueryTuple, JsonPathQueryResult> jsonPathQueryResultCache;

    private Cache<String, OPERATIONALTEMPLATE> atOptCache;
    private final Cache<UUID, I_QueryOptMetaData> queryOptMetaDataCache;

    //index uuid to templateId
    private Map<UUID, String> idxCacheUuidToTemplateId = new ConcurrentHashMap<>();
    //index templateId to uuid
    private Map<String, UUID> idxCacheTemplateIdToUuid = new ConcurrentHashMap<>();

    private Set<String> allTemplateId = new HashSet<>();

    private Map<String, Set<String>> nodeIdsByTemplateIdMap = new HashMap<>();


    private final CacheManager cacheManager;

    @Value("${system.allow-template-overwrite:false}")
    private boolean allowTemplateOverwrite;

    @Autowired
    public KnowledgeCacheService(@Qualifier("templateDBStorageService") TemplateStorage templateStorage, CacheManager cacheManager) {
        this.templateStorage = templateStorage;
        this.cacheManager = cacheManager;

        atOptCache = cacheManager.getCache(OPERATIONAL_TEMPLATE_CACHE, String.class, OPERATIONALTEMPLATE.class);
        queryOptMetaDataCache = cacheManager.getCache(CacheConfiguration.INTROSPECT_CACHE, UUID.class, I_QueryOptMetaData.class);
        jsonPathQueryResultCache = cacheManager.getCache(QUERY_CACHE, TemplateIdQueryTuple.class, JsonPathQueryResult.class);
    }

    @PostConstruct
    public void init() {
        allTemplateId = listAllOperationalTemplates().stream().map(TemplateMetaData::getOperationaltemplate).map(OPERATIONALTEMPLATE::getTemplateId).map(OBJECTID::getValue).collect(Collectors.toSet());
        listAllOperationalTemplates().stream().map(TemplateMetaData::getOperationaltemplate)
                .forEach(this::putIntoCache);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        ExecutorService executorService =
                MoreExecutors.getExitingExecutorService(executor,
                        100, TimeUnit.MILLISECONDS);
        executor.submit(() ->
                allTemplateId.forEach(this::precalculateQuerys));

    }

    @PreDestroy
    public void closeCache() {
        cacheManager.close();
    }

    @Override
    public Set<String> getAllTemplateIds() {
        return allTemplateId;
    }

    @Override
    public String addOperationalTemplate(byte[] content) {

        InputStream inputStream = new ByteArrayInputStream(content);

        TemplateDocument document;
        try {
            document = TemplateDocument.Factory.parse(inputStream);
        } catch (XmlException | IOException e) {
            throw new InvalidApiParameterException(e.getMessage());
        }
        OPERATIONALTEMPLATE template = document.getTemplate();

        if (template == null) {
            throw new InvalidApiParameterException("Could not parse input template");
        }

        if (template.getConcept() == null || template.getConcept().isEmpty())
            throw new IllegalArgumentException("Supplied template has nil or empty concept");

        if (template.getDefinition() == null || template.getDefinition().isNil())
            throw new IllegalArgumentException("Supplied template has nil or empty definition");

        if (template.getDescription() == null || !template.getDescription().validate())
            throw new IllegalArgumentException("Supplied template has nil or empty description");

        //get the filename from the template template Id
        Optional<TEMPLATEID> filenameOptional = Optional.ofNullable(template.getTemplateId());
        String templateId = filenameOptional.orElseThrow(() -> new InvalidApiParameterException("Invalid template input content")).getValue();


        // pre-check: if already existing throw proper exception
        if (!allowTemplateOverwrite && retrieveOperationalTemplate(templateId).isPresent()) {
            throw new StateConflictException("Operational template with this template ID already exists");
        }

        templateStorage.storeTemplate(template);


        invalidateCache(template);

        putIntoCache(template);

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        ExecutorService executorService =
                MoreExecutors.getExitingExecutorService(executor,
                        100, TimeUnit.MILLISECONDS);
        executor.submit(() -> precalculateQuerys(templateId));


        //retrieve the template Id for this new entry
        return template.getTemplateId().getValue();
    }

    private void putIntoCache(OPERATIONALTEMPLATE template) {
        String templateId = template.getTemplateId().getValue();
        atOptCache.put(templateId, template);
        idxCacheUuidToTemplateId.put(UUID.fromString(template.getUid().getValue()), templateId);
        idxCacheTemplateIdToUuid.put(templateId, UUID.fromString(template.getUid().getValue()));
        allTemplateId.add(templateId);

        getQueryOptMetaData(templateId);
    }

    private void precalculateQuerys(String templateId) {
        I_QueryOptMetaData queryOptMetaData = getQueryOptMetaData(templateId);

        Sets.powerSet(queryOptMetaData.getContainmentSet())
                .stream()
                .filter(s -> !s.isEmpty())
                .map(s -> new JsonPathQueryBuilder(new ArrayList<>(s)))
                .map(JsonPathQueryBuilder::assemble)
                .forEach(s -> resolveForTemplate(templateId, s));
    }


    // invalidates some derived caches like the queryOptMetaDataCache which depend on the template
    private void invalidateCache(OPERATIONALTEMPLATE template) {

        //invalidate the cache for this template
        queryOptMetaDataCache.remove(UUID.fromString(template.getUid().getValue()));
        Set<TemplateIdQueryTuple> collect = StreamSupport.stream(jsonPathQueryResultCache.spliterator(), true)
                .map(Cache.Entry::getKey)

                .filter(k -> k.getTemplateId().equals(template.getTemplateId().getValue()))
                .collect(Collectors.toSet());
        jsonPathQueryResultCache.removeAll(collect);
    }


    @Override
    public List<TemplateMetaData> listAllOperationalTemplates() {
        return templateStorage.listAllOperationalTemplates();
    }


    @Override
    public Optional<OPERATIONALTEMPLATE> retrieveOperationalTemplate(String key) {
        log.debug("retrieveOperationalTemplate({})", key);
        OPERATIONALTEMPLATE template = atOptCache.get(key);
        if (template == null) {     // null if not in cache already, which triggers the following retrieval and putting into cache
            template = getOperationaltemplateFromFileStorage(key);
        }
        return Optional.ofNullable(template);
    }

    @Override
    public Optional<OPERATIONALTEMPLATE> retrieveOperationalTemplate(UUID uuid) {
        String key = findTemplateIdByUuid(uuid);
        if (key == null) {
            return Optional.empty();
        }

        return retrieveOperationalTemplate(key);
    }

    private String findTemplateIdByUuid(UUID uuid) {
        String templateId = idxCacheUuidToTemplateId.get(uuid);

        if (templateId == null) {
            templateId = listAllOperationalTemplates()
                    .stream()
                    .filter(t -> t.getErrorList().isEmpty())
                    .filter(t -> t.getOperationaltemplate().getUid().getValue().equals(uuid.toString()))
                    .map(t -> t.getOperationaltemplate().getTemplateId().getValue())
                    .findFirst()
                    .orElse(null);
            idxCacheUuidToTemplateId.put(uuid, templateId);
        }

        return templateId;
    }

    private UUID findUuidByTemplateId(String templateId) {
        UUID uuid = idxCacheTemplateIdToUuid.get(templateId);
        if (uuid == null) {
            uuid = UUID.fromString(retrieveOperationalTemplate(templateId)
                    .orElseThrow()
                    .getUid()
                    .getValue());
            idxCacheTemplateIdToUuid.put(templateId, uuid);
        }
        return uuid;
    }


    @Override
    public I_QueryOptMetaData getQueryOptMetaData(UUID uuid) {

        final I_QueryOptMetaData retval;

        if (queryOptMetaDataCache.containsKey(uuid))
            retval = queryOptMetaDataCache.get(uuid);
        else {
            retval = buildAndCacheQueryOptMetaData(uuid);
        }
        return retval;
    }

    @Override
    public I_QueryOptMetaData getQueryOptMetaData(String templateId) {

        return getQueryOptMetaData(findUuidByTemplateId(templateId));
    }

    private I_QueryOptMetaData buildAndCacheQueryOptMetaData(UUID uuid) {
        I_QueryOptMetaData retval;
        Optional<OPERATIONALTEMPLATE> operationaltemplate = Optional.empty();
        try {
            operationaltemplate = retrieveOperationalTemplate(uuid);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        if (operationaltemplate.isPresent()) {
            retval = buildAndCacheQueryOptMetaData(operationaltemplate.get());
        } else {
            throw new IllegalArgumentException("Could not retrieve  knowledgeCacheService.getKnowledgeCache() cache for template Uid:" + uuid);
        }
        return retval;
    }

    private I_QueryOptMetaData buildAndCacheQueryOptMetaData(OPERATIONALTEMPLATE operationaltemplate) {
        log.info("Updating getQueryOptMetaData cache for template: {}", operationaltemplate.getTemplateId().getValue());
        final I_QueryOptMetaData visitor;
        try {
            Map map = new OptVisitor().traverse(operationaltemplate);
            visitor = QueryOptMetaData.getInstance(new MapJson(map).toJson());
        } catch (Exception e) {
            throw new InternalServerException(e.getMessage(), e);
        }

        queryOptMetaDataCache.put(UUID.fromString(operationaltemplate.getUid().getValue()), visitor);
        return visitor;
    }

    /**
     * Helper function to retrieve the operational template from file storage and put it into the cache. For instance,
     * to handle first time access to an operational template before it was written to cache already.
     *
     * @param filename of the OPT file in storage
     * @return The operational template or null.
     */
    private OPERATIONALTEMPLATE getOperationaltemplateFromFileStorage(String filename) {
        OPERATIONALTEMPLATE operationaltemplate = templateStorage.readOperationaltemplate(filename).orElse(null);
        if (operationaltemplate != null) {
            atOptCache.put(filename, operationaltemplate);      // manual putting into cache (actual opt cache and then id cache)
            idxCacheUuidToTemplateId.put(UUID.fromString(operationaltemplate.getUid().getValue()), filename);
        }
        return operationaltemplate;
    }

    @Override
    public boolean containsNodeIds(String templateId, Collection<String> nodeIds) {
        Set<String> templateNodeIds = nodeIdsByTemplateIdMap.computeIfAbsent(templateId, t -> getQueryOptMetaData(t).getAllNodeIds());
        return templateNodeIds.containsAll(nodeIds);
    }

    @Override
    public JsonPathQueryResult resolveForTemplate(String templateId, String jsonQueryExpression) {
        TemplateIdQueryTuple key = new TemplateIdQueryTuple(templateId, jsonQueryExpression);

        JsonPathQueryResult jsonPathQueryResult = jsonPathQueryResultCache.get(key);


        if (jsonPathQueryResult == null) {
            Map<String, Object> evaluate = new OptJsonPath(this).evaluate(templateId, jsonQueryExpression);
            if (!MapUtils.isEmpty(evaluate)) {
                jsonPathQueryResult = new JsonPathQueryResult(templateId, evaluate);
            } else {
                //dummy result since null can not be path of a cache
                jsonPathQueryResult = new JsonPathQueryResult(null, Collections.emptyMap());
            }
            jsonPathQueryResultCache.put(key, jsonPathQueryResult);
        }

        if (jsonPathQueryResult.getTemplateId() != null) {
            return jsonPathQueryResult;
        }
        // Is dummy result
        else {

            return null;
        }

    }

    @Override
    public I_KnowledgeCache getKnowledge() {
        return this;
    }
}
