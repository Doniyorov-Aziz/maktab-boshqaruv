package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A single piece of text: a reply to a parent, or a reason for rejecting a request. */
public class TextRequestDto {

    @NotBlank
    @Size(max = 3000)
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
