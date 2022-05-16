package com.dio.pedido.controller;

import java.util.Date;
import java.util.UUID;

import com.dio.pedido.business.dto.PedidoProcessadoResponseDTO;
import com.dio.pedido.business.dto.PedidoRequestDTO;
import com.dio.pedido.servicebus.QueueSender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
@AllArgsConstructor
public final class PedidoController {

    private static final int TEMPO_PROCESSAMENTO = 500;

    private static final String PAGAMENTO_BASE_URL = "http://localhost:8182";
    private static final String REALIZAR_PAGAMENTO_API = "/api/dio/v1/realizarPagamentoCompleto";

    private QueueSender queueSender;

    @PostMapping("v1/realizarPedido")
    public ResponseEntity<PedidoProcessadoResponseDTO> realizarPedido(final @RequestBody PedidoRequestDTO pedido) {

        UUID pedidoId = processar(pedido);

        PedidoProcessadoResponseDTO response = new PedidoProcessadoResponseDTO(pedidoId.toString());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("v1/realizarPedidoCompleto")
    public ResponseEntity<PedidoProcessadoResponseDTO> realizarPedidoCompleto(
            final @RequestBody PedidoRequestDTO pedido) {

        ResponseEntity<PedidoProcessadoResponseDTO> response = realizarPedido(pedido);

        ResponseEntity<PedidoProcessadoResponseDTO> pagamentoResponse = realizarPagamento(pedido);

        if (pagamentoResponse.getStatusCode() == HttpStatus.OK) {

            String mensagemPedido = response.getBody().getMessage();
            String mensagemPagemento = pagamentoResponse.getBody().getMessage();

            response.getBody().setMessage(mensagemPedido.concat("|").concat(mensagemPagemento));
        }

        return response;
    }

    @PostMapping("v1/realizarPedidoCompletoCoreografia")
    public ResponseEntity<PedidoProcessadoResponseDTO> realizarPedidoCompletoCoreografia(
            final @RequestBody PedidoRequestDTO pedido) {

        ResponseEntity<PedidoProcessadoResponseDTO> response = realizarPedido(pedido);

        queueSender.send(pedidoToJson(pedido));

        return response;
    }

    private String pedidoToJson(PedidoRequestDTO pedido) {

        ObjectMapper mapper = new ObjectMapper();

        try {

            return mapper.writeValueAsString(pedido);

        } catch (JsonProcessingException e) {

            e.printStackTrace();
        }

        return null;
    }

    private ResponseEntity<PedidoProcessadoResponseDTO> realizarPagamento(PedidoRequestDTO pedido) {

        WebClient client = WebClient.create(PAGAMENTO_BASE_URL);

        Mono<PedidoProcessadoResponseDTO> responsePagamento = client.post().uri(REALIZAR_PAGAMENTO_API)
                .body(Mono.just(pedido), PedidoRequestDTO.class).retrieve()
                .bodyToMono(PedidoProcessadoResponseDTO.class);

        PedidoProcessadoResponseDTO response = responsePagamento.block();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private UUID processar(PedidoRequestDTO pedido) {

        try {

            pedido.setId(UUID.randomUUID());
            pedido.setDataPedido(new Date());

            log.info("Processando pedido...");
            log.debug("Pedido número: {}", pedido.getId().toString());
            log.debug("Pedido completo: {}", pedido.toString());

            Thread.sleep(TEMPO_PROCESSAMENTO);

            log.info("Estado 1: Pedido em processamento");

        } catch (InterruptedException e) {

            log.warn("Interrupted!", e);

            Thread.currentThread().interrupt();
        }

        return pedido.getId();
    }
}
