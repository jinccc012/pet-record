package com.harumi.petrecord.healthrecord;

import com.harumi.petrecord.file.FileCategory;
import com.harumi.petrecord.file.FileRepository;
import com.harumi.petrecord.file.FileResource;
import com.harumi.petrecord.file.FileStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/sql/truncate-all.sql", executionPhase = BEFORE_TEST_METHOD)
class HealthRecordFlowIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired FileRepository fileRepository;

    private record Registered(String token, long userId) {
    }

    private Registered registerToken(String username) throws Exception {
        String body = String.format("""
                {"username":"%s","email":"%s@example.com","password":"password123"}
                """, username, username);
        MvcResult r = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        JsonNode json = mapper.readTree(r.getResponse().getContentAsByteArray());
        return new Registered(json.get("accessToken").asText(), json.get("user").get("id").asLong());
    }

    private long createPet(String token) throws Exception {
        MvcResult r = mvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mochi\",\"species\":\"CAT\"}"))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(r.getResponse().getContentAsByteArray()).get("id").asLong();
    }

    private long insertFile(long uploadedBy, FileCategory category) {
        FileResource f = FileResource.builder()
                .uploadedBy(uploadedBy).originalFilename("doc.pdf").storedFilename("uuid.pdf")
                .storageProvider("R2").bucketName("test-bucket").objectKey("k/" + uploadedBy)
                .contentType(category == FileCategory.HEALTH_REPORT ? "application/pdf" : "image/webp")
                .fileSize(1024L).fileCategory(category).status(FileStatus.ACTIVE)
                .build();
        return fileRepository.save(f).getId();
    }

    @Test
    void fullHealthRecordFlow() throws Exception {
        Registered alice = registerToken("alice");
        long petId = createPet(alice.token());
        long fileA = insertFile(alice.userId(), FileCategory.HEALTH_REPORT);
        long fileB = insertFile(alice.userId(), FileCategory.HEALTH_IMAGE);
        String base = "/api/pets/" + petId + "/health-records";

        String createBody = """
                {
                  "visitDate": "2026-05-25",
                  "hospitalName": "Mochi Vet",
                  "doctorName": "Dr. X",
                  "medicalNote": "Annual checkup",
                  "attachedFileIds": [%d, %d]
                }
                """.formatted(fileA, fileB);
        MvcResult created = mvc.perform(post(base)
                        .header("Authorization", "Bearer " + alice.token())
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hospitalName").value("Mochi Vet"))
                .andExpect(jsonPath("$.attachments.length()").value(2))
                .andReturn();
        long recordId = mapper.readTree(created.getResponse().getContentAsByteArray()).get("id").asLong();

        // list
        mvc.perform(get(base).header("Authorization", "Bearer " + alice.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // get
        mvc.perform(get(base + "/" + recordId).header("Authorization", "Bearer " + alice.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attachments.length()").value(2));

        // PUT - replace attachments to just one + update fields
        String updateBody = """
                {
                  "visitDate": "2026-05-26",
                  "hospitalName": "New Hospital",
                  "doctorName": null,
                  "medicalNote": "Follow-up",
                  "attachedFileIds": [%d]
                }
                """.formatted(fileB);
        mvc.perform(put(base + "/" + recordId)
                        .header("Authorization", "Bearer " + alice.token())
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitDate").value("2026-05-26"))
                .andExpect(jsonPath("$.hospitalName").value("New Hospital"))
                .andExpect(jsonPath("$.attachments.length()").value(1))
                .andExpect(jsonPath("$.attachments[0].fileId").value((int) fileB));

        // delete -> 204, then 404
        mvc.perform(delete(base + "/" + recordId).header("Authorization", "Bearer " + alice.token()))
                .andExpect(status().isNoContent());
        mvc.perform(get(base + "/" + recordId).header("Authorization", "Bearer " + alice.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotAttachFileNotOwned() throws Exception {
        Registered alice = registerToken("alice2");
        Registered bob = registerToken("bob2");
        long bobPet = createPet(bob.token());
        long aliceFile = insertFile(alice.userId(), FileCategory.HEALTH_REPORT);

        String body = """
                {"visitDate":"2026-05-25","attachedFileIds":[%d]}
                """.formatted(aliceFile);
        // Bob tries to attach Alice's file -> 400
        mvc.perform(post("/api/pets/" + bobPet + "/health-records")
                        .header("Authorization", "Bearer " + bob.token())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crossUserCannotAccessOthersHealthRecords() throws Exception {
        Registered alice = registerToken("alice3");
        Registered bob = registerToken("bob3");
        long alicePet = createPet(alice.token());
        String base = "/api/pets/" + alicePet + "/health-records";

        // Bob lists Alice's pet records -> 404 (pet not owned)
        mvc.perform(get(base).header("Authorization", "Bearer " + bob.token()))
                .andExpect(status().isNotFound());

        // Missing visitDate -> 400
        mvc.perform(post(base).header("Authorization", "Bearer " + alice.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
