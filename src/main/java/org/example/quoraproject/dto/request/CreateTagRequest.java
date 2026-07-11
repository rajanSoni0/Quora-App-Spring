package org.example.quoraproject.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTagRequest(

        @NotBlank(message = "Tag name is required")
        @Size(min = 2, max = 60, message = "Tag name must be between 2 and 60 characters")
        @Pattern(regexp = "^[a-zA-Z0-9 +#.-]+$", message = "Tag name contains invalid characters")
        String name
) {}
