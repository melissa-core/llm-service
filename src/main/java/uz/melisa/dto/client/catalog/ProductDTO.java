package uz.melisa.dto.client.catalog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDTO {

    private Long id;
    private String name;
    @ToString.Exclude
    private String description;
    @ToString.Exclude
    private Long price;
    @ToString.Exclude
    private String organizationName;
    @ToString.Exclude
    private String category;
    @ToString.Exclude
    private Set<String> tags;
}
