/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH), Jake Smolka (Hannover Medical School), and Luis Marco-Ruiz (Hannover Medical School).
 *
 * This file is part of project EHRbase
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

package org.ehrbase.api.service;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.ehr.VersionedComposition;
import com.nedap.archie.rm.generic.RevisionHistory;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.response.ehrscape.CompositionDto;
import org.ehrbase.response.ehrscape.CompositionFormat;
import org.ehrbase.response.ehrscape.StructuredString;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface CompositionService extends BaseService {
    /**
     * @param compositionId The {@link UUID} of the composition to be returned.
     * @param version       The version to returned. If null return the latest
     * @return
     * @throws InternalServerException
     */
    Optional<CompositionDto> retrieve(UUID compositionId, Integer version);

    /**
     * TODO: untested because not needed, yet
     *
     * Gets the composition that is closest in time before timestamp
     *
     * @param compositionId UUID (versioned_object_id) of composition
     * @param timestamp Given time
     * @return Optional of CompositionDto closest in time before timestamp
     */
    Optional<CompositionDto> retrieveByTimestamp(UUID compositionId, LocalDateTime timestamp);

    /**
     * Public serializer entry point which will be called with
     * composition dto fetched from database and the
     * desired target serialized string format.
     * Will parse the composition dto into target format either
     * with a custom lambda expression for desired target format
     *
     * @param composition Composition dto from database
     * @param format      Target format
     * @return Structured string with string of data and content format
     */
    StructuredString serialize(CompositionDto composition, CompositionFormat format);

    /**
     * Overloaded wrapper function to create composition with minimal set of input. TemplateID is read from composition content.
     *
     * @param ehrId - Target EHR
     * @param content - String representation of content
     * @param format - Format of data within the string representation
     * @return - UUID of new created composition entry
     * @throws InternalServerException
     */
    UUID create(UUID ehrId, String content, CompositionFormat format);

    /**
     * Creates a deserialized representation of the composition data from
     * source format and stores these data into the corresponding tables.
     *
     * @param ehrId      - Target EHR
     * @param content    - String representation of content
     * @param format     - Format of data within the string representation
     * @param templateId - Template id for usage with Marand's composition converter
     * @param linkUid    - UUID of link for compo_xref entry master
     * @return - UUID of new created composition entry
     * @throws InternalServerException
     */
    UUID create(UUID ehrId, String content, CompositionFormat format, String templateId, UUID linkUid);

    /**
     * Creates a composition which will be connected to the given existing contribution. Unlike with the general create() methods, where
     * the contribution will be created ad hoc.
     * @param ehrId Target EHR
     * @param composition Composition as RM object
     * @param contributionId ID of the contribution this composition is part of
     * @return UUID of newly created composition
     * @throws InternalServerException when creation failed
     */
    UUID create(UUID ehrId, Composition composition, UUID contributionId);

    /**
     * Overloaded wrapper function to update composition with minimal set of input. TemplateID is read from composition content.
     *
     * @param compositionId
     * @param format
     * @param content
     * @return Versioned id string of updated composition
     * @throws InternalServerException when updating failed
     * @throws ObjectNotFoundException when targeted composition couldn't be found
     */
    String update(UUID compositionId, CompositionFormat format, String content);

    /**
     * Updates an existing composition entry with new data. Implicitly created new contribution ad-hoc.
     *
     * @param compositionId - Target composition UUID to update
     * @param format        - Source format of content
     * @param content       - String representation of payload data
     * @param templateId    - Corresponding template id
     * @return - Versioned id string of updated composition
     * @throws InternalServerException when updating failed
     * @throws ObjectNotFoundException when targeted composition couldn't be found
     */
    String update(UUID compositionId, CompositionFormat format, String content, String templateId);

    /**
     * Updates a composition which will be connected to the given existing contribution. Unlike with the general update() methods, where
     * the contribution will be created ad hoc.
     * @param compositionId Target composition UUID to update
     * @param composition Composition as RM object
     * @param contributionId ID of the contribution this composition is part of
     * @return Versioned id string of updated composition
     * @throws InternalServerException when updating failed
     * @throws ObjectNotFoundException when targeted composition couldn't be found
     */
    String update(UUID compositionId, Composition composition, UUID contributionId);

    /**
     * Deletes a composition, i.e. creates a new version with deleted status. Return time of deletion.
     * @param compositionId - Target composition UUID
     * @return Time of deletion on database level
     * @throws ObjectNotFoundException  when targeted composition couldn't be found
     * @throws InternalServerException when deletion failed
     */
    LocalDateTime delete(UUID compositionId);

    /**
     * Deletes a composition which will be connected to the given existing contribution. Unlike with the general delete() methods, where
     * the contribution will be created ad hoc.
     * @param compositionId Target composition UUID
     * @param contributionId Custom contribution UUID
     * @return Time of deletion, if successful
     */
    LocalDateTime delete(UUID compositionId, UUID contributionId);

    Integer getLastVersionNumber(UUID compositionId);

    /**
     * Helper function to read UUID from given composition input in stated format.
     * @param content Composition input
     * @param format Composition format
     * @return The UUID or null when not available.
     */
    String getUidFromInputComposition(String content, CompositionFormat format);

    /**
     * Helper function to read the template ID from given composition input in stated format.
     * @param content Composition input
     * @param format Composition format
     * @return The UUID or null when not available.
     */
    String getTemplateIdFromInputComposition(String content, CompositionFormat format);

    /**
     * Gets the version of a composition that is closest in time before timestamp
     * @param compositionId UUID (versioned_object_id) of composition
     * @param timestamp Given time
     * @return Version closest in time before given timestamp, or `null` in case of error.
     */
    Integer getVersionByTimestamp(UUID compositionId, LocalDateTime timestamp);

    /**
     * Checks if given ID is a valid composition ID.
     * @param versionedObjectId ID to check
     * @return True if ID exists
     * @throws ObjectNotFoundException if ID does not exist
     */
    boolean exists(UUID versionedObjectId);

    /**
     * Checks if given composition ID is ID of a logically deleted composition.
     * @param versionedObjectId ID to check
     * @return True if deleted, false if not
     */
    boolean isDeleted(UUID versionedObjectId);

    /**
     * Admin method to delete a Composition from the DB. See EHRbase Admin API specification for details.
     * @param compositionId Composition to delete
     */
    void adminDelete(UUID compositionId);

    /**
     * Gets version container Composition associated with given EHR and Composition ID.
     * @param ehrUid Given EHR ID
     * @param composition Given Composition ID
     * @return Version container object
     */
    VersionedComposition getVersionedComposition(UUID ehrUid, UUID composition);

    /**
     * Gets revision history of given composition.
     * @param composition Given composition.
     * @return Revision history
     */
    RevisionHistory getRevisionHistoryOfVersionedComposition(UUID composition);

    /**
     * Gets Original Version container class representation of the given composition at given version.
     * @param versionedObjectUid Given composition Uid.
     * @param version Given version number.
     * @return Original Version container class representation.
     */
    Optional<OriginalVersion<Composition>> getOriginalVersionComposition(UUID versionedObjectUid, int version);
}
