package uz.melisa.dto.client.translator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TranslatorTextRequestDTO {

    private String text;
    private String source;
    private String target;
    private String script;
}
