package test.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import test.IntegrationTest;
import test.domain.Conference;
import test.repository.ConferenceRepository;
import test.repository.search.ConferenceSearchRepository;

/**
 * Integration tests for the {@link ConferenceResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ConferenceResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final Instant DEFAULT_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/conferences";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/_search/conferences";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ConferenceRepository conferenceRepository;

    /**
     * This repository is mocked in the test.repository.search test package.
     *
     * @see test.repository.search.ConferenceSearchRepositoryMockConfiguration
     */
    @Autowired
    private ConferenceSearchRepository mockConferenceSearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restConferenceMockMvc;

    private Conference conference;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Conference createEntity(EntityManager em) {
        Conference conference = new Conference().name(DEFAULT_NAME).date(DEFAULT_DATE);
        return conference;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Conference createUpdatedEntity(EntityManager em) {
        Conference conference = new Conference().name(UPDATED_NAME).date(UPDATED_DATE);
        return conference;
    }

    @BeforeEach
    public void initTest() {
        conference = createEntity(em);
    }

    @Test
    @Transactional
    void createConference() throws Exception {
        int databaseSizeBeforeCreate = conferenceRepository.findAll().size();
        // Create the Conference
        restConferenceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(conference))
            )
            .andExpect(status().isCreated());

        // Validate the Conference in the database
        List<Conference> conferenceList = conferenceRepository.findAll();
        assertThat(conferenceList).hasSize(databaseSizeBeforeCreate + 1);
        Conference testConference = conferenceList.get(conferenceList.size() - 1);
        assertThat(testConference.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testConference.getDate()).isEqualTo(DEFAULT_DATE);

        // Validate the Conference in Elasticsearch
        verify(mockConferenceSearchRepository, times(1)).save(testConference);
    }

    @Test
    @Transactional
    void createConferenceWithExistingId() throws Exception {
        // Create the Conference with an existing ID
        conference.setId(1L);

        int databaseSizeBeforeCreate = conferenceRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        restConferenceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(conference))
            )
            .andExpect(status().isBadRequest());

        // Validate the Conference in the database
        List<Conference> conferenceList = conferenceRepository.findAll();
        assertThat(conferenceList).hasSize(databaseSizeBeforeCreate);

        // Validate the Conference in Elasticsearch
        verify(mockConferenceSearchRepository, times(0)).save(conference);
    }

    @Test
    @Transactional
    void getAllConferences() throws Exception {
        // Initialize the database
        conferenceRepository.saveAndFlush(conference);

        // Get all the conferenceList
        restConferenceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(conference.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].date").value(hasItem(DEFAULT_DATE.toString())));
    }

    @Test
    @Transactional
    void getConference() throws Exception {
        // Initialize the database
        conferenceRepository.saveAndFlush(conference);

        // Get the conference
        restConferenceMockMvc
            .perform(get(ENTITY_API_URL_ID, conference.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(conference.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.date").value(DEFAULT_DATE.toString()));
    }

    @Test
    @Transactional
    void getNonExistingConference() throws Exception {
        // Get the conference
        restConferenceMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putNewConference() throws Exception {
        // Initialize the database
        conferenceRepository.saveAndFlush(conference);

        int databaseSizeBeforeUpdate = conferenceRepository.findAll().size();

        // Update the conference
        Conference updatedConference = conferenceRepository.findById(conference.getId()).get();
        // Disconnect from session so that the updates on updatedConference are not directly saved in db
        em.detach(updatedConference);
        updatedConference.name(UPDATED_NAME).date(UPDATED_DATE);

        restConferenceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedConference.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(updatedConference))
            )
            .andExpect(status().isOk());

        // Validate the Conference in the database
        List<Conference> conferenceList = conferenceRepository.findAll();
        assertThat(conferenceList).hasSize(databaseSizeBeforeUpdate);
        Conference testConference = conferenceList.get(conferenceList.size() - 1);
        assertThat(testConference.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testConference.getDate()).isEqualTo(UPDATED_DATE);

        // Validate the Conference in Elasticsearch
        verify(mockConferenceSearchRepository).save(testConference);
    }

    @Test
    @Transactional
    void putNonExistingConference() throws Exception {
        int databaseSizeBeforeUpdate = conferenceRepository.findAll().size();
        conference.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restConferenceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, conference.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(conference))
            )
            .andExpect(status().isBadRequest());

        // Validate the Conference in the database
        List<Conference> conferenceList = conferenceRepository.findAll();
        assertThat(conferenceList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Conference in Elasticsearch
        verify(mockConferenceSearchRepository, times(0)).save(conference);
    }

    @Test
    @Transactional
    void putWithIdMismatchConference() throws Exception {
        int databaseSizeBeforeUpdate = conferenceRepository.findAll().size();
        conference.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restConferenceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, count.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(conference))
            )
            .andExpect(status().isBadRequest());

        // Validate the Conference in the database
        List<Conference> conferenceList = conferenceRepository.findAll();
        assertThat(conferenceList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Conference in Elasticsearch
        verify(mockConferenceSearchRepository, times(0)).save(conference);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamConference() throws Exception {
        int databaseSizeBeforeUpdate = conferenceRepository.findAll().size();
        conference.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restConferenceMockMvc
            .perform(
                put(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(conference))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Conference in the database
        List<Conference> conferenceList = conferenceRepository.findAll();
        assertThat(conferenceList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Conference in Elasticsearch
        verify(mockConferenceSearchRepository, times(0)).save(conference);
    }

    @Test
    @Transactional
    void partialUpdateConferenceWithPatch() throws Exception {
        // Initialize the database
        conferenceRepository.saveAndFlush(conference);

        int databaseSizeBeforeUpdate = conferenceRepository.findAll().size();

        // Update the conference using partial update
        Conference partialUpdatedConference = new Conference();
        partialUpdatedConference.setId(conference.getId());

        restConferenceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedConference.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedConference))
            )
            .andExpect(status().isOk());

        // Validate the Conference in the database
        List<Conference> conferenceList = conferenceRepository.findAll();
        assertThat(conferenceList).hasSize(databaseSizeBeforeUpdate);
        Conference testConference = conferenceList.get(conferenceList.size() - 1);
        assertThat(testConference.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testConference.getDate()).isEqualTo(DEFAULT_DATE);
    }

    @Test
    @Transactional
    void fullUpdateConferenceWithPatch() throws Exception {
        // Initialize the database
        conferenceRepository.saveAndFlush(conference);

        int databaseSizeBeforeUpdate = conferenceRepository.findAll().size();

        // Update the conference using partial update
        Conference partialUpdatedConference = new Conference();
        partialUpdatedConference.setId(conference.getId());

        partialUpdatedConference.name(UPDATED_NAME).date(UPDATED_DATE);

        restConferenceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedConference.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedConference))
            )
            .andExpect(status().isOk());

        // Validate the Conference in the database
        List<Conference> conferenceList = conferenceRepository.findAll();
        assertThat(conferenceList).hasSize(databaseSizeBeforeUpdate);
        Conference testConference = conferenceList.get(conferenceList.size() - 1);
        assertThat(testConference.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testConference.getDate()).isEqualTo(UPDATED_DATE);
    }

    @Test
    @Transactional
    void patchNonExistingConference() throws Exception {
        int databaseSizeBeforeUpdate = conferenceRepository.findAll().size();
        conference.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restConferenceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, conference.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(conference))
            )
            .andExpect(status().isBadRequest());

        // Validate the Conference in the database
        List<Conference> conferenceList = conferenceRepository.findAll();
        assertThat(conferenceList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Conference in Elasticsearch
        verify(mockConferenceSearchRepository, times(0)).save(conference);
    }

    @Test
    @Transactional
    void patchWithIdMismatchConference() throws Exception {
        int databaseSizeBeforeUpdate = conferenceRepository.findAll().size();
        conference.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restConferenceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, count.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(conference))
            )
            .andExpect(status().isBadRequest());

        // Validate the Conference in the database
        List<Conference> conferenceList = conferenceRepository.findAll();
        assertThat(conferenceList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Conference in Elasticsearch
        verify(mockConferenceSearchRepository, times(0)).save(conference);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamConference() throws Exception {
        int databaseSizeBeforeUpdate = conferenceRepository.findAll().size();
        conference.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restConferenceMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(conference))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Conference in the database
        List<Conference> conferenceList = conferenceRepository.findAll();
        assertThat(conferenceList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Conference in Elasticsearch
        verify(mockConferenceSearchRepository, times(0)).save(conference);
    }

    @Test
    @Transactional
    void deleteConference() throws Exception {
        // Initialize the database
        conferenceRepository.saveAndFlush(conference);

        int databaseSizeBeforeDelete = conferenceRepository.findAll().size();

        // Delete the conference
        restConferenceMockMvc
            .perform(delete(ENTITY_API_URL_ID, conference.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Conference> conferenceList = conferenceRepository.findAll();
        assertThat(conferenceList).hasSize(databaseSizeBeforeDelete - 1);

        // Validate the Conference in Elasticsearch
        verify(mockConferenceSearchRepository, times(1)).deleteById(conference.getId());
    }

    @Test
    @Transactional
    void searchConference() throws Exception {
        // Configure the mock search repository
        // Initialize the database
        conferenceRepository.saveAndFlush(conference);
        when(mockConferenceSearchRepository.search("id:" + conference.getId())).thenReturn(Stream.of(conference));

        // Search the conference
        restConferenceMockMvc
            .perform(get(ENTITY_SEARCH_API_URL + "?query=id:" + conference.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(conference.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].date").value(hasItem(DEFAULT_DATE.toString())));
    }
}
