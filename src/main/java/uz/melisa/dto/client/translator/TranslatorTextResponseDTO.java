package uz.melisa.dto.client.translator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TranslatorTextResponseDTO {

    private List<String> translations;
}
