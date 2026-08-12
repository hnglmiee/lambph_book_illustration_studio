package com.hoanglam.bis.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RunStyleStepRequest {
    private String userStyle; // optional — để trống thì Gemini tự chọn
}