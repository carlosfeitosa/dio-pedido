package com.dio.pedido.business.dto;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public final class PedidoRequestDTO {

    private UUID id;

    private String nomeCliente;

    private String numeroCartao;

    private Date dataPedido;

    private List<String> itens;
}
