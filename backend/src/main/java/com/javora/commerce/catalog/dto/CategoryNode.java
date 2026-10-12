package com.javora.commerce.catalog.dto;

import java.io.Serializable;
import java.util.List;

public record CategoryNode(Long id, String name, String slug, List<CategoryNode> children) implements Serializable {
}
