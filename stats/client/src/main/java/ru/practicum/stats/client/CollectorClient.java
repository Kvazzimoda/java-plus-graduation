package ru.practicum.stats.client;

import com.google.protobuf.Empty;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.practicum.stats.proto.UserActionControllerGrpc;
import ru.practicum.stats.proto.UserActionProto;

@Slf4j
@Service
public class CollectorClient {

    @GrpcClient("collector")
    UserActionControllerGrpc.UserActionControllerBlockingStub userActionClient;

    public void sendUserAction(UserActionProto userAction) {
        log.info("отправка действия пользователя через клиент в контроллер коллектора");
        Empty empty = userActionClient.collectUserAction(userAction);
        log.info("действие пользователя через клиент в контроллер коллектора отправлено");
    }
}
