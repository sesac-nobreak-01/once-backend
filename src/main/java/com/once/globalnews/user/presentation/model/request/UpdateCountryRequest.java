package com.once.globalnews.user.presentation.model.request;

import jakarta.validation.constraints.NotEmpty;

public record UpdateCountryRequest(
    @NotEmpty String country
) {
}
