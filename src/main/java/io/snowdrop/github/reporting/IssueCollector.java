package io.snowdrop.github.reporting;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.snowdrop.BotException;
import io.snowdrop.github.Github;
import io.snowdrop.github.reporting.model.Issue;
import io.snowdrop.github.reporting.model.PullRequest;
import io.snowdrop.github.reporting.model.Repository;

public class IssueCollector {

  private static final Logger LOGGER = LoggerFactory.getLogger(IssueCollector.class);
  private static final SimpleDateFormat DF = new SimpleDateFormat("dd/MM/yyyy");

  private final GitHubClient client;
  private final RepositoryService repositoryService;
  private final IssueService issueService;
  private final int reportingDay;
  private final int reportingHour;

  private final Set<String> users;
  private final Set<String> organizations;

  // These dates represent current reporting period
  private final ZonedDateTime startTime;
  private final ZonedDateTime endTime;

  // These represent the oldest reporting period possible
  private final Date minStartTime;
  private final Date minEndTime;

  public IssueCollector(GitHubClient client, int reportingDay, int reportingHour, Set<String> users,
      Set<String> organizations) {
    this.client = client;
    this.repositoryService = new RepositoryService(client);
    this.issueService = new IssueService(client);
    this.reportingDay = reportingDay;
    this.reportingHour = reportingHour;
    this.users = users;
    this.organizations = organizations;

    this.endTime = ZonedDateTime.now().with(DayOfWeek.of(reportingDay)).withHour(reportingHour);
    this.startTime = endTime.minusWeeks(1);

    this.minStartTime = Date.from(startTime.minusMonths(6).toInstant());
    this.minEndTime = Date.from(endTime.toInstant());
    init();
  }

  public void init() {
  }

  public void refresh() {
    LOGGER.info("Refreshing reporting data.");
    collectIssues();
  }

  public Map<String, Set<Issue>> collectIssues() {
    return Repository.<Repository>streamAll()
      .map(Repository::getParent).filter(r -> r != null)
      .distinct()
      .sorted()
      .flatMap(i -> teamIssueStream(i, "all"))
      .collect(Collectors.groupingBy(Issue::getAssignee, Collectors.toSet()));
  }

  public Set<Issue> userIssues(String user, Repository repository, String state) {
    synchronized (client) {

      try {
        String id = repository.isFork() ? repository.getParent() : repository.getOwner() + "/" + repository.getName();
        LOGGER.info("Getting {} issues for repository: {}/{} during: {} - {}.", state, Github.user(id), Github.repo(id), DF.format(minStartTime), DF.format(minEndTime));
        return issueService.getIssues(Github.user(id), Github.repo(id), Github.params().state(state).build())
          .stream()
          .map(i -> Issue.create(id, i))
          .filter(i -> i.isActiveDuring(minStartTime, minEndTime))
          .map(IssueCollector::log)
          .collect(Collectors.toSet());
      } catch (IOException e) {
        throw BotException.launderThrowable(e);
      }
    }
  }

  private Map<String, Set<Issue>> teamIssues(String repository, String state) {
    return teamIssueStream(repository, state).collect(Collectors.groupingBy(Issue::getCreator, Collectors.toSet()));
  }

  private Stream<Issue> teamIssueStream(String repository, String state) {
    synchronized (client) {
      try {
        LOGGER.info("Getting {} issues for repository: {}", state, repository);
        return issueService
            .getIssues(Github.user(repository), Github.repo(repository), Github.params().state(state).build()).stream()
            .filter(i -> i.getAssignee() != null && users.contains(i.getAssignee().getLogin()))
            .map(i -> Issue.create(repository, i)).filter(i -> i.isActiveDuring(minStartTime, minEndTime))
            .map(IssueCollector::log);
      } catch (IOException e) {
        throw BotException.launderThrowable(e);
      }
    }
  }

  /**
   * Log and return self. Lambda friendly log hack.
   */
  private static Issue log(Issue issue) {
    LOGGER.info("{}: {}. {} - {} - {}.", issue.getNumber(), issue.getTitle(), DF.format(issue.getCreatedAt()),
                issue.getUpdatedAt() != null ? DF.format(issue.getUpdatedAt()) : null,
                issue.getClosedAt() != null ? DF.format(issue.getClosedAt()) : null);
    return issue;
  }

  public Set<String> getUsers() {
    return users;
  }

  public Set<String> getOrganizations() {
    return organizations;
  }

  public Stream<Issue> getIssues() {
    return Issue.<Issue>findAll().stream();
  }

  public ZonedDateTime getStartTime() {
    return startTime;
  }

  public ZonedDateTime getEndTime() {
    return endTime;
  }
}
