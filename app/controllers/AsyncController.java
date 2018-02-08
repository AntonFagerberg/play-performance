package controllers;

import akka.actor.ActorSystem;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.Duration;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

@Singleton
public class AsyncController extends Controller {

    private final ActorSystem actorSystem;
    private final ExecutionContextExecutor exec;

    @Inject
    public AsyncController(ActorSystem actorSystem, ExecutionContextExecutor exec) {
        this.actorSystem = actorSystem;
        this.exec = exec;
    }

    public Result syncBlocking() {
        String username = getUserFromDatabaseBlocking();
        int postCount = getPostCountFromHTTPBlocking();
        int followers = getFollowerCountFromCacheBlocking();

        return Results.ok(hello(username, postCount, followers));
    }

    public CompletionStage<Result> asyncBlocking() {
        CompletionStage<String> user = CompletableFuture.supplyAsync(this::getUserFromDatabaseBlocking);
        CompletionStage<Integer> postCount = CompletableFuture.supplyAsync(this::getPostCountFromHTTPBlocking);
        CompletionStage<Integer> followerCount = CompletableFuture.supplyAsync(this::getFollowerCountFromCacheBlocking);

        return user.thenComposeAsync(username ->
                postCount.thenComposeAsync(posts ->
                        followerCount.thenApplyAsync(followers ->
                                hello(username, posts, followers)
                        )
                )
        ).thenApplyAsync(Results::ok, exec);
    }

    public CompletionStage<Result> asyncNonBlocking() {
        CompletionStage<String> user = getUserFromDatabaseNonBlocking();
        CompletionStage<Integer> postCount = getPostCountFromHTTPNonBlocking();
        CompletionStage<Integer> followerCount = getFollowerCountFromCacheNonBlocking();

        return user.thenComposeAsync(username ->
                postCount.thenComposeAsync(posts ->
                        followerCount.thenApplyAsync(followers ->
                                hello(username, posts, followers)
                        )
                )
        ).thenApplyAsync(Results::ok, exec);
    }

    private String hello(String username, int posts, int followers) {
        return "Hello " + username + ", you have " + posts + " posts and " + followers + " followers!";
    }

    private String getUserFromDatabaseBlocking() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "Anton";
    }

    private int getPostCountFromHTTPBlocking() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return 100;
    }

    private int getFollowerCountFromCacheBlocking() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return 2000;
    }

    private CompletionStage<String> getUserFromDatabaseNonBlocking() {
        return getFutureMessage(200, TimeUnit.MILLISECONDS, "Anton");
    }

    private CompletionStage<Integer> getPostCountFromHTTPNonBlocking() {
        return getFutureMessage(500, TimeUnit.MILLISECONDS, 100);
    }

    private CompletionStage<Integer> getFollowerCountFromCacheNonBlocking() {
        return getFutureMessage(300, TimeUnit.MILLISECONDS, 2000);
    }

    private <T> CompletionStage<T> getFutureMessage(long time, TimeUnit timeUnit, T value) {
        CompletableFuture<T> future = new CompletableFuture<>();
        actorSystem.scheduler().scheduleOnce(
                Duration.create(time, timeUnit),
                () -> future.complete(value),
                exec
        );
        return future;
    }

}
