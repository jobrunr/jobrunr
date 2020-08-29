/**
 * All info on how to enqueue and schedule background jobs.
 * <p>
 * In the JavaDoc the convention regarding generics as follows:
 * <ul>
 *     <li><strong><S></strong>: this represents a service (e.g. available in your IoC container)</li>
 *     <li><strong><T></strong>: this represents an item for which you want to schedule a job</li>
 * </ul>
 *
 * <strong>An example:</strong>
 * <pre>{@code
 *     Stream<User> userStream = userRepository.getAllUsers();
 *     BackgroundJob.<SomeService, User>enqueue(userStream, (x, user) -> x.doWork("do some work for user " + user.getId()));
 * }</pre>
 */
package org.jobrunr.scheduling;