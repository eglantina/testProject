package test.web.rest;

import static org.elasticsearch.index.query.QueryBuilders.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;
import test.domain.Conference;
import test.repository.ConferenceRepository;
import test.repository.search.ConferenceSearchRepository;
import test.web.rest.errors.BadRequestAlertException;

/**
 * REST controller for managing {@link test.domain.Conference}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class ConferenceResource {

    private final Logger log = LoggerFactory.getLogger(ConferenceResource.class);

    private static final String ENTITY_NAME = "testmsConference";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ConferenceRepository conferenceRepository;

    private final ConferenceSearchRepository conferenceSearchRepository;

    public ConferenceResource(ConferenceRepository conferenceRepository, ConferenceSearchRepository conferenceSearchRepository) {
        this.conferenceRepository = conferenceRepository;
        this.conferenceSearchRepository = conferenceSearchRepository;
    }

    /**
     * {@code POST  /conferences} : Create a new conference.
     *
     * @param conference the conference to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new conference, or with status {@code 400 (Bad Request)} if the conference has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/conferences")
    public ResponseEntity<Conference> createConference(@RequestBody Conference conference) throws URISyntaxException {
        log.debug("REST request to save Conference : {}", conference);
        if (conference.getId() != null) {
            throw new BadRequestAlertException("A new conference cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Conference result = conferenceRepository.save(conference);
        conferenceSearchRepository.save(result);
        return ResponseEntity
            .created(new URI("/api/conferences/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /conferences/:id} : Updates an existing conference.
     *
     * @param id the id of the conference to save.
     * @param conference the conference to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated conference,
     * or with status {@code 400 (Bad Request)} if the conference is not valid,
     * or with status {@code 500 (Internal Server Error)} if the conference couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/conferences/{id}")
    public ResponseEntity<Conference> updateConference(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Conference conference
    ) throws URISyntaxException {
        log.debug("REST request to update Conference : {}, {}", id, conference);
        if (conference.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, conference.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!conferenceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Conference result = conferenceRepository.save(conference);
        conferenceSearchRepository.save(result);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, conference.getId().toString()))
            .body(result);
    }

    /**
     * {@code PATCH  /conferences/:id} : Partial updates given fields of an existing conference, field will ignore if it is null
     *
     * @param id the id of the conference to save.
     * @param conference the conference to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated conference,
     * or with status {@code 400 (Bad Request)} if the conference is not valid,
     * or with status {@code 404 (Not Found)} if the conference is not found,
     * or with status {@code 500 (Internal Server Error)} if the conference couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/conferences/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<Conference> partialUpdateConference(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody Conference conference
    ) throws URISyntaxException {
        log.debug("REST request to partial update Conference partially : {}, {}", id, conference);
        if (conference.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, conference.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!conferenceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Conference> result = conferenceRepository
            .findById(conference.getId())
            .map(existingConference -> {
                if (conference.getName() != null) {
                    existingConference.setName(conference.getName());
                }
                if (conference.getDate() != null) {
                    existingConference.setDate(conference.getDate());
                }

                return existingConference;
            })
            .map(conferenceRepository::save)
            .map(savedConference -> {
                conferenceSearchRepository.save(savedConference);

                return savedConference;
            });

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, conference.getId().toString())
        );
    }

    /**
     * {@code GET  /conferences} : get all the conferences.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of conferences in body.
     */
    @GetMapping("/conferences")
    public List<Conference> getAllConferences() {
        log.debug("REST request to get all Conferences");
        return conferenceRepository.findAll();
    }

    /**
     * {@code GET  /conferences/:id} : get the "id" conference.
     *
     * @param id the id of the conference to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the conference, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/conferences/{id}")
    public ResponseEntity<Conference> getConference(@PathVariable Long id) {
        log.debug("REST request to get Conference : {}", id);
        Optional<Conference> conference = conferenceRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(conference);
    }

    /**
     * {@code DELETE  /conferences/:id} : delete the "id" conference.
     *
     * @param id the id of the conference to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/conferences/{id}")
    public ResponseEntity<Void> deleteConference(@PathVariable Long id) {
        log.debug("REST request to delete Conference : {}", id);
        conferenceRepository.deleteById(id);
        conferenceSearchRepository.deleteById(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code SEARCH  /_search/conferences?query=:query} : search for the conference corresponding
     * to the query.
     *
     * @param query the query of the conference search.
     * @return the result of the search.
     */
    @GetMapping("/_search/conferences")
    public List<Conference> searchConferences(@RequestParam String query) {
        log.debug("REST request to search Conferences for query {}", query);
        return StreamSupport.stream(conferenceSearchRepository.search(query).spliterator(), false).collect(Collectors.toList());
    }
}
