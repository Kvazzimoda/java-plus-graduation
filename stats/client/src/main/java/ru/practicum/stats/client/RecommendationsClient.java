package ru.practicum.stats.client;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.practicum.stats.proto.*;

import java.util.List;

@Slf4j
@Service
public class RecommendationsClient {

    @GrpcClient("analyzer")
    RecommendationsControllerGrpc.RecommendationsControllerBlockingStub recommendationClient;

    public List<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        return Lists.newArrayList(recommendationClient.getRecommendationsForUser(request));
    }

    public List<RecommendedEventProto> getSimilarEvent(SimilarEventsRequestProto request) {
        return Lists.newArrayList(recommendationClient.getSimilarEvents(request));
    }

    public List<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        return Lists.newArrayList(recommendationClient.getInteractionsCount(request));
    }
}
