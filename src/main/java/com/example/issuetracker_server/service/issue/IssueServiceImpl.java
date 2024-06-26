package com.example.issuetracker_server.service.issue;

import com.example.issuetracker_server.domain.issue.Issue;
import com.example.issuetracker_server.domain.issue.IssueRepository;
import com.example.issuetracker_server.domain.issue.Priority;
import com.example.issuetracker_server.domain.issue.State;
import com.example.issuetracker_server.domain.member.Member;
import com.example.issuetracker_server.domain.member.MemberRepository;
import com.example.issuetracker_server.domain.memberproject.MemberProject;
import com.example.issuetracker_server.domain.memberproject.MemberProjectRepository;
import com.example.issuetracker_server.domain.memberproject.Role;
import com.example.issuetracker_server.domain.project.Project;
import com.example.issuetracker_server.domain.project.ProjectRepository;
import com.example.issuetracker_server.dto.issue.IssueCreateRequestDto;
import com.example.issuetracker_server.dto.issue.IssueResponseDto;
import com.example.issuetracker_server.dto.issue.IssueStatisticResponseDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IssueServiceImpl implements IssueService {

    private final IssueRepository issueRepository;

    private final MemberRepository memberRepository;

    private final ProjectRepository projectRepository;

    private final MemberProjectRepository memberProjectRepository;

    public Optional<Issue> getIssue(Long issueId) {
        return issueRepository.findById(issueId);
    }

    @Override
    @Transactional
    public boolean createIssue(Long projectId, String memberId, IssueCreateRequestDto request) {
        try {
            Optional<Project> project = projectRepository.findById(projectId);
            Optional<Member> member = memberRepository.findById(memberId);
            if (project.isEmpty() || member.isEmpty())
                return false;

            Issue issue = new Issue();
            issue.setProject(project.get());
            issue.setReporter(member.get());
            issue.setTitle(request.getTitle());
            issue.setDescription(request.getDescription());
            issue.setState(State.NEW);
            issue.setPriority(Priority.MAJOR);
            issueRepository.save(issue);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<IssueResponseDto> getIssues(Long projectId, String filterBy, String filterValue) {
        List<Issue> issues;

        if ((filterBy == null || filterBy.isEmpty()) || (filterValue == null || filterValue.isEmpty()))
//            issues = issueRepository.findAll();
            issues = issueRepository.findByProjectId(projectId);
        else {
            switch (Objects.requireNonNull(filterBy).toLowerCase()) {
                case "title":
                    issues = issueRepository.findByProjectIdAndTitleContainingIgnoreCase(projectId, filterValue);
                    break;
                case "reporter":
                    issues = issueRepository.findByProjectIdAndReporterIdContainingIgnoreCase(projectId, filterValue);
                    break;
                case "assignee":
                    issues = issueRepository.findByProjectIdAndAssigneeIdContainingIgnoreCase(projectId, filterValue);
                    break;
                case "state":
                    issues = issueRepository.findByProjectIdAndState(projectId, State.valueOf(filterValue));
                    break;
                default:
                    throw new IllegalArgumentException("Invalid filter criteria");
            }
        }
        return issues.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private IssueResponseDto toDto(Issue issue) {
        return IssueResponseDto.builder()
                .id(issue.getId())
                .project_id(issue.getProject().getId())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .reporter_id(issue.getReporter().getId())
                .assignee_id(issue.getAssignee() != null ? issue.getAssignee().getId() : null)
                .fixer_id(issue.getFixer() != null ? issue.getFixer().getId() : null)
                .priority(issue.getPriority())
                .state(issue.getState())
                .created_date(issue.getCreatedDate() != null ? issue.getCreatedDate().toString() : null)
                .modified_date(issue.getModifiedDate() != null ? issue.getModifiedDate().toString() : null)
                .build();
    }

    @Override
    public Optional<IssueResponseDto> getIssue(Long projectId, Long issueId) {
        Optional<Issue> issue = issueRepository.findById(issueId);
        return issue.map(this::toDto);
    }

    @Override
    public IssueStatisticResponseDto getStatistic(Long ProjectId) {
        List<Issue> issues = issueRepository.findByProjectId(ProjectId);

        int dayIssues = (int) issues.stream()
                .filter(issue -> issue.getCreatedDate().toLocalDate().equals(LocalDate.now()))
                .count();

        int monthIssues = (int) issues.stream()
                .filter(issue -> issue.getCreatedDate().toLocalDate().getMonth() == LocalDate.now().getMonth() &&
                        issue.getCreatedDate().toLocalDate().getYear() == LocalDate.now().getYear())
                .count();

        int totalIssues = issues.size();

        int closedIssues = (int) issues.stream()
                .filter(issue -> issue.getState() == State.CLOSED)
                .count();

        return new IssueStatisticResponseDto(dayIssues, monthIssues, totalIssues, closedIssues);
    }

    @Override
    public Map<String, List<String>> getRecommendAssignee(Long projectId, Long issueId) {
        List<Member> devs = memberProjectRepository.findByProjectIdAndRole(projectId, Role.DEV).stream()
                .map(MemberProject::getMember).toList();
        Map<Member, Long> assigneeCount = devs.stream()
                .collect(Collectors.toMap(member -> member, member -> 0L));

        List<Issue> issues = issueRepository.findByProjectId(projectId).stream()
                .filter(issue -> issue.getFixer() == null && issue.getAssignee() != null)
                .toList();

        // 이슈의 assignee에 따라 assigneeCount 맵의 값을 증가
        issues.forEach(issue -> {
            Member assignee = issue.getAssignee();
            assigneeCount.put(assignee, assigneeCount.get(assignee) + Priority.toValue(issue.getPriority()));
        });

        // 결과를 이슈 수에 따라 오름차순으로 정렬
        List<Map.Entry<Member, Long>> sortedAssigneeList = assigneeCount.entrySet().stream()
                .sorted(Map.Entry.<Member, Long>comparingByValue()
                        .thenComparing(entry -> entry.getKey().getId()))
                .toList();
//        // 정렬된 리스트 출력
//        sortedAssigneeList.forEach(entry ->
//                System.out.println("Member ID: " + entry.getKey().getId() + ", Score: " + entry.getValue())
//        );

        Map<String, List<String>> response = new HashMap<>();
        response.put("dev_ids", sortedAssigneeList.stream()
                .limit(5)
                .map(entry -> entry.getKey().getId())
                .collect(Collectors.toList()));
        return response;
    }

    @Override
    @Transactional
    public boolean assignIssue(Long projectId, Long issueId, String memberId, Priority priority) {
        Optional<MemberProject> memberProject = memberProjectRepository.findByMemberIdAndProjectId(memberId, projectId);
        if (memberProject.isEmpty() || memberProject.get().getRole() != Role.DEV)
            return false;
        Member assignee = memberProject.get().getMember();

        Optional<Issue> issue = issueRepository.findById(issueId);
        if (issue.isEmpty() || !Objects.equals(issue.get().getProject().getId(), projectId))
            return false;

        issue.get().setAssignee(assignee);
        issue.get().setPriority(priority);
        if (issue.get().getState() == State.NEW)
            issue.get().setState(State.ASSIGNED);
        issueRepository.save(issue.get());
        return true;
    }

    @Override
    @Transactional
    public boolean updateIssue(String id, Long projectId, Long issueId, String title, String description) {
        Optional<Issue> issue = issueRepository.findById(issueId);
        if (issue.isEmpty() || !Objects.equals(issue.get().getProject().getId(), projectId)
                || !issue.get().getReporter().getId().equals(id))
            return false;

        issue.get().setTitle(title);
        issue.get().setDescription(description);
        issueRepository.save(issue.get());
        return true;
    }

    @Override
    @Transactional
    public boolean updateIssueState(Long projectId, Long issueId, String id, Role role, State state) {
        Optional<Issue> issue = issueRepository.findById(issueId);
        if (issue.isEmpty() || !Objects.equals(issue.get().getProject().getId(), projectId))
            return false;

        if (role == Role.PL) {
            issue.get().setState(state);
            issueRepository.save(issue.get());
            return true;
        } else if (role == Role.DEV && issue.get().getAssignee() != null
                && issue.get().getAssignee().getId().equals(id)
                && ((issue.get().getState() == State.ASSIGNED) || (issue.get().getState() == State.REOPEN)) && state == State.FIXED) {
            issue.get().setState(State.FIXED);
            issueRepository.save(issue.get());
            return true;
        } else if (role == Role.TESTER && issue.get().getAssignee() != null
                && issue.get().getReporter().getId().equals(id)
                && issue.get().getState() == State.FIXED && state == State.RESOLVED) {
            issue.get().setState(State.RESOLVED);
            issueRepository.save(issue.get());
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean deleteIssue(Long projectId, Long issueId) {
        Optional<Issue> issue = issueRepository.findById(issueId);
        if (issue.isEmpty() || !Objects.equals(issue.get().getProject().getId(), projectId)) {
            return false;
        }
        issueRepository.delete(issue.get());

        return true;
    }
}