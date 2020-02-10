package io.snowdrop.github.reporting;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class GithubReportingFactory {

  @ConfigProperty(name = "github.users")
  Set<String> users;

  @ConfigProperty(name = "github.reporting.organizations")
  Set<String> organizations;

  @ConfigProperty(name = "github.reporting.day-of-week", defaultValue = "4")
  int reportingDayOfWeek;

  @ConfigProperty(name = "github.reporting.hours", defaultValue = "12")
  int reportingHours;

  @Inject
  GitHubClient client;

  @Produces
  public RepositoryCollector createRepositoryCollector() {
    return new RepositoryCollector(client, users, organizations);
  }

  @Produces
  public IssueCollector createIssueCollector() {
    return new IssueCollector(client, reportingDayOfWeek, reportingHours, users, organizations);
  }

  @Produces
  public PullRequestCollector createPullRequestCollector() {
    return new PullRequestCollector(client, reportingDayOfWeek, reportingHours, users, organizations);
  }

}
