package uz.melisa.dto.client.fitrat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FitratTransliterateRequestDTO {

    private String text;
    private String from;
    private String to;
}
