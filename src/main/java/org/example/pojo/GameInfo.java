package org.example.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.ConcurrentHashMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GameInfo {
    private volatile float progress = 0;
    private ConcurrentHashMap<String, Integer> player2Score = new ConcurrentHashMap<>();

    public void reset() {
        progress = 0;
        player2Score.clear();
    }

    public void toIndex() {
        reset();
    }
}
