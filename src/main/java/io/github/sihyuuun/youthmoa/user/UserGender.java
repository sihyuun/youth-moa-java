package io.github.sihyuuun.youthmoa.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserGender {
    MALE("남"),
    FEMALE("여");

    private final String label;
}
