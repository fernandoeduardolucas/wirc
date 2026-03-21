package com.wirc.bootstrap;

import java.util.List;

public record ChatRoomSeed(
        String id,
        String name,
        List<String> participants
) {
}
