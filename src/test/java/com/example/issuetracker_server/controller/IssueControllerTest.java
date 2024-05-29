package com.example.issuetracker_server.controller;

import com.example.issuetracker_server.domain.issue.State;
import com.example.issuetracker_server.domain.memberproject.Role;
import com.example.issuetracker_server.dto.issue.IssueCreateRequestDto;
import com.example.issuetracker_server.dto.issue.IssueResponseDto;
import com.example.issuetracker_server.service.issue.IssueService;
import com.example.issuetracker_server.service.member.MemberService;
import com.example.issuetracker_server.service.memberproject.MemberProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
public class IssueControllerTest {

    private MockMvc mockMvc;

    @Mock
    private IssueService issueService;

    @Mock
    private MemberService memberService;

    @Mock
    private MemberProjectService memberProjectService;

    @InjectMocks
    private IssueController issueController;

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(issueController).build();
    }

    @Test
    public void testCreateIssueUnauthorized() throws Exception {
        // Given
        String id = "testuser";
        String pw = "password";
        Long projectId = 1L;

        when(memberService.login(id, pw)).thenReturn(false);

        // When
        mockMvc.perform(post("/project/" + projectId + "/issue")
                        .param("id", id)
                        .param("pw", pw)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"issue title\", \"description\": \"issue description\"}"))

                // Then
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testCreateIssueForbidden() throws Exception {
        // Given
        String id = "testuser";
        String pw = "password";
        Long projectId = 1L;

        when(memberService.login(id, pw)).thenReturn(true);
        when(memberProjectService.getRole(id, projectId)).thenReturn(Optional.of(Role.DEV));
        boolean result = memberService.login(id, pw);

        // When
        mockMvc.perform(post("/project/" + projectId + "/issue")
                        .param("id", id)
                        .param("pw", pw)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"issue title\", \"description\": \"issue description\"}"))

                // Then
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCreateIssueBadRequest() throws Exception {
        // Given
        String id = "testuser";
        String pw = "password";
        Long projectId = 1L;

        when(memberService.login(id, pw)).thenReturn(true);
        when(memberProjectService.getRole(id, projectId)).thenReturn(Optional.of(Role.TESTER));
        when(issueService.createIssue(eq(projectId), eq(id), any(IssueCreateRequestDto.class))).thenReturn(false);

        // When
        mockMvc.perform(post("/project/" + projectId + "/issue")
                        .param("id", id)
                        .param("pw", pw)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"issue title\", \"description\": \"issue description\"}"))

                // Then
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateIssueSuccess() throws Exception {
        // Given
        String id = "testuser";
        String pw = "password";
        Long projectId = 1L;

        when(memberService.login(id, pw)).thenReturn(true);
        when(memberProjectService.getRole(id, projectId)).thenReturn(Optional.of(Role.TESTER));
        when(issueService.createIssue(eq(projectId), eq(id), any(IssueCreateRequestDto.class))).thenReturn(true);

        // When
        mockMvc.perform(post("/project/" + projectId + "/issue")
                        .param("id", id)
                        .param("pw", pw)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"issue title\", \"description\": \"issue description\"}"))

                // Then
                .andExpect(status().isOk());
    }

    @Test
    public void testGetIssuesUnauthorized() throws Exception {
        // Given
        String id = "testuser";
        String pw = "password";
        Long projectId = 1L;

        when(memberService.login(id, pw)).thenReturn(false);

        // When
        mockMvc.perform(get("/project/" + projectId + "/issue")
                        .param("id", id)
                        .param("pw", pw)
                        .param("sort", "title")
                        .param("order", "asc")
                        .contentType(MediaType.APPLICATION_JSON))

                // Then
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetIssuesForbidden() throws Exception {
        // Given
        String id = "testuser";
        String pw = "password";
        Long projectId = 1L;

        when(memberService.login(id, pw)).thenReturn(true);
        when(memberProjectService.getRole(id, projectId)).thenReturn(Optional.empty());

        // When
        mockMvc.perform(get("/project/" + projectId + "/issue")
                        .param("id", id)
                        .param("pw", pw)
                        .param("sort", "title")
                        .param("order", "asc")
                        .contentType(MediaType.APPLICATION_JSON))

                // Then
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetIssuesBadRequest() throws Exception {
        // Given
        String id = "testuser";
        String pw = "password";
        Long projectId = 1L;

        when(memberService.login(id, pw)).thenReturn(true);
        when(memberProjectService.getRole(id, projectId)).thenReturn(Optional.of(Role.TESTER));
        when(issueService.getIssues(projectId, "title", "asc")).thenThrow(new RuntimeException());

        // When
        mockMvc.perform(get("/project/" + projectId + "/issue")
                        .param("id", id)
                        .param("pw", pw)
                        .param("sort", "title")
                        .param("order", "asc")
                        .contentType(MediaType.APPLICATION_JSON))

                // Then
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetIssuesSuccess() throws Exception {
        // Given
        String id = "testuser";
        String pw = "password";
        Long projectId = 1L;

        when(memberService.login(id, pw)).thenReturn(true);
        when(memberProjectService.getRole(id, projectId)).thenReturn(Optional.of(Role.TESTER));
        when(issueService.getIssues(projectId, "title", "asc")).thenReturn(Arrays.asList(
                new IssueResponseDto(1L, projectId, "Issue 1", "Description 1", id, null, null, null, null, "2022-01-01", "2022-01-01"),
                new IssueResponseDto(2L, projectId, "Issue 2", "Description 2", id, null, null, null, null, "2022-01-02", "2022-01-02")
        ));

        // When
        mockMvc.perform(get("/project/" + projectId + "/issue")
                        .param("id", id)
                        .param("pw", pw)
                        .param("sort", "title")
                        .param("order", "asc")
                        .contentType(MediaType.APPLICATION_JSON))

                // Then
                .andExpect(status().isOk());
    }

    @Test
    public void testGetIssuesSuccessTitleAsc() throws Exception {
        // Given
        String id = "testuser";
        String pw = "password";
        Long projectId = 1L;

        when(memberService.login(id, pw)).thenReturn(true);
        when(memberProjectService.getRole(id, projectId)).thenReturn(Optional.of(Role.TESTER));
        when(issueService.getIssues(projectId, "title", "asc")).thenReturn(Arrays.asList(
                new IssueResponseDto(1L, projectId, "A Issue", "Description A", id, null, null, null, null, "2022-01-01", "2022-01-01"),
                new IssueResponseDto(2L, projectId, "B Issue", "Description B", id, null, null, null, null, "2022-01-02", "2022-01-02")
        ));

        // When
        mockMvc.perform(get("/project/" + projectId + "/issue")
                        .param("id", id)
                        .param("pw", pw)
                        .param("sort", "title")
                        .param("order", "asc")
                        .contentType(MediaType.APPLICATION_JSON))

                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[0].title", is("A Issue")))
                .andExpect(jsonPath("$[1].title", is("B Issue")));
    }

    @Test
    public void testGetIssuesSuccessTitleDesc() throws Exception {
        // Given
        String id = "testuser";
        String pw = "password";
        Long projectId = 1L;

        when(memberService.login(id, pw)).thenReturn(true);
        when(memberProjectService.getRole(id, projectId)).thenReturn(Optional.of(Role.TESTER));
        when(issueService.getIssues(projectId, "title", "desc")).thenReturn(Arrays.asList(
                new IssueResponseDto(1L, projectId, "B Issue", "Description B", id, null, null, null, null, "2022-01-02", "2022-01-02"),
                new IssueResponseDto(2L, projectId, "A Issue", "Description A", id, null, null, null, null, "2022-01-01", "2022-01-01")
        ));

        // When
        mockMvc.perform(get("/project/" + projectId + "/issue")
                        .param("id", id)
                        .param("pw", pw)
                        .param("sort", "title")
                        .param("order", "desc")
                        .contentType(MediaType.APPLICATION_JSON))

                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[0].title", is("B Issue")))
                .andExpect(jsonPath("$[1].title", is("A Issue")));
    }

    @Test
    public void testGetIssuesSuccessCreatedDateAsc() throws Exception {
        // Given
        String id = "testuser";
        String pw = "password";
        Long projectId = 1L;

        when(memberService.login(id, pw)).thenReturn(true);
        when(memberProjectService.getRole(id, projectId)).thenReturn(Optional.of(Role.TESTER));
        when(issueService.getIssues(projectId, "createdDate", "asc")).thenReturn(Arrays.asList(
                new IssueResponseDto(1L, projectId, "Issue 1", "Description 1", id, null, null, null, null, "2022-01-01", "2022-01-01"),
                new IssueResponseDto(2L, projectId, "Issue 2", "Description 2", id, null, null, null, null, "2022-01-02", "2022-01-02")
        ));

        // When
        mockMvc.perform(get("/project/" + projectId + "/issue")
                        .param("id", id)
                        .param("pw", pw)
                        .param("sort", "createdDate")
                        .param("order", "asc")
                        .contentType(MediaType.APPLICATION_JSON))

                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[0].createdDate", is("2022-01-01")))
                .andExpect(jsonPath("$[1].createdDate", is("2022-01-02")));
    }

    @Test
    public void testGetIssuesSuccessCreatedDateDesc() throws Exception {
        // Given
        String id = "testuser";
        String pw = "password";
        Long projectId = 1L;

        when(memberService.login(id, pw)).thenReturn(true);
        when(memberProjectService.getRole(id, projectId)).thenReturn(Optional.of(Role.TESTER));
        when(issueService.getIssues(projectId, "createdDate", "desc")).thenReturn(Arrays.asList(
                new IssueResponseDto(1L, projectId, "Issue 2", "Description 2", id, null, null, null, null, "2022-01-02", "2022-01-02"),
                new IssueResponseDto(2L, projectId, "Issue 1", "Description 1", id, null, null, null, null, "2022-01-01", "2022-01-01")
        ));

        // When
        mockMvc.perform(get("/project/" + projectId + "/issue")
                        .param("id", id)
                        .param("pw", pw)
                        .param("sort", "createdDate")
                        .param("order", "desc")
                        .contentType(MediaType.APPLICATION_JSON))

                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[0].createdDate", is("2022-01-02")))
                .andExpect(jsonPath("$[1].createdDate", is("2022-01-01")));
    }

    @Test
    public void testGetIssuesSuccessStateAsc() throws Exception {
        // Given
        String id = "testuser";
        String pw = "password";
        Long projectId = 1L;

        when(memberService.login(id, pw)).thenReturn(true);
        when(memberProjectService.getRole(id, projectId)).thenReturn(Optional.of(Role.TESTER));
        when(issueService.getIssues(projectId, "state", "asc")).thenReturn(Arrays.asList(
                new IssueResponseDto(1L, projectId, "Issue 1", "Description 1", id, null, null, null, State.NEW, "2022-01-01", "2022-01-01"),
                new IssueResponseDto(2L, projectId, "Issue 2", "Description 2", id, null, null, null, State.ASSIGNED, "2022-01-02", "2022-01-02")
        ));

        // When
        mockMvc.perform(get("/project/" + projectId + "/issue")
                        .param("id", id)
                        .param("pw", pw)
                        .param("sort", "state")
                        .param("order", "asc")
                        .contentType(MediaType.APPLICATION_JSON))

                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[0].state", is("NEW")))
                .andExpect(jsonPath("$[1].state", is("ASSIGNED")));
    }

    @Test
    public void testGetIssuesSuccessStateDesc() throws Exception {
        // Given
        String id = "testuser";
        String pw = "password";
        Long projectId = 1L;

        when(memberService.login(id, pw)).thenReturn(true);
        when(memberProjectService.getRole(id, projectId)).thenReturn(Optional.of(Role.TESTER));
        when(issueService.getIssues(projectId, "state", "desc")).thenReturn(Arrays.asList(
                new IssueResponseDto(1L, projectId, "Issue 2", "Description 2", id, null, null, null, State.ASSIGNED, "2022-01-02", "2022-01-02"),
                new IssueResponseDto(2L, projectId, "Issue 1", "Description 1", id, null, null, null, State.NEW, "2022-01-01", "2022-01-01")
        ));

        // When
        mockMvc.perform(get("/project/" + projectId + "/issue")
                        .param("id", id)
                        .param("pw", pw)
                        .param("sort", "state")
                        .param("order", "desc")
                        .contentType(MediaType.APPLICATION_JSON))

                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[0].state", is("ASSIGNED")))
                .andExpect(jsonPath("$[1].state", is("NEW")));
    }

    @Test
    public void testGetIssueUnauthorized() throws Exception {
        // Given
        String id = "testuser";
        String pw = "password";
        Long projectId = 1L;
        Long issueId = 1L;

        when(memberService.login(id, pw)).thenReturn(false);

        // When
        mockMvc.perform(get("/project/" + projectId + "/issue/" + issueId)
                        .param("id", id)
                        .param("pw", pw)
                        .contentType(MediaType.APPLICATION_JSON))

                // Then
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetIssueForbidden() throws Exception {
        // Given
        String id = "testuser";
        String pw = "password";
        Long projectId = 1L;
        Long issueId = 1L;

        when(memberService.login(id, pw)).thenReturn(true);
        when(memberProjectService.getRole(id, projectId)).thenReturn(Optional.empty());

        // When
        mockMvc.perform(get("/project/" + projectId + "/issue/" + issueId)
                        .param("id", id)
                        .param("pw", pw)
                        .contentType(MediaType.APPLICATION_JSON))

                // Then
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetIssueNotFound() throws Exception {
        // Given
        String id = "testuser";
        String pw = "password";
        Long projectId = 1L;
        Long issueId = 1L;

        when(memberService.login(id, pw)).thenReturn(true);
        when(memberProjectService.getRole(id, projectId)).thenReturn(Optional.of(Role.TESTER));
        when(issueService.getIssue(projectId, issueId)).thenReturn(Optional.empty());

        // When
        mockMvc.perform(get("/project/" + projectId + "/issue/" + issueId)
                        .param("id", id)
                        .param("pw", pw)
                        .contentType(MediaType.APPLICATION_JSON))

                // Then
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetIssueSuccess() throws Exception {
        // Given
        String id = "testuser";
        String pw = "password";
        Long projectId = 1L;
        Long issueId = 1L;

        IssueResponseDto issueResponseDto = new IssueResponseDto(1L, projectId, "Issue 1", "Description 1", id, null, null, null, null, "2022-01-01", "2022-01-01");

        when(memberService.login(id, pw)).thenReturn(true);
        when(memberProjectService.getRole(id, projectId)).thenReturn(Optional.of(Role.TESTER));
        when(issueService.getIssue(projectId, issueId)).thenReturn(Optional.of(issueResponseDto));

        // When
        mockMvc.perform(get("/project/" + projectId + "/issue/" + issueId)
                        .param("id", id)
                        .param("pw", pw)
                        .contentType(MediaType.APPLICATION_JSON))

                // Then
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.title", is("Issue 1")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description", is("Description 1")));
    }
}
