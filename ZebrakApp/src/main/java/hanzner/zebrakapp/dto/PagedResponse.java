package hanzner.zebrakapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Generická stránkovaná odpověď API")
public class PagedResponse<T> {

    @Schema(description = "Seznam prvků na aktuální stránce")
    private List<T> content;

    @Schema(description = "Číslo aktuální stránky (indexováno od 0)", example = "0")
    private int page;

    @Schema(description = "Počet prvků na stránku", example = "20")
    private int size;

    @Schema(description = "Celkový počet nalezených prvků", example = "42")
    private long totalElements;

    @Schema(description = "Celkový počet dostupných stránek", example = "3")
    private int totalPages;

    @Schema(description = "Příznak, zda se jedná o první stránku", example = "true")
    private boolean first;

    @Schema(description = "Příznak, zda se jedná o poslední stránku", example = "false")
    private boolean last;

    @Schema(description = "Příznak, zda existuje následující stránka", example = "true")
    private boolean hasNext;

    @Schema(description = "Příznak, zda existuje předchozí stránka", example = "false")
    private boolean hasPrevious;

    public static <T> PagedResponse<T> of(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    public static <T, R> PagedResponse<R> of(Page<T> page, Function<T, R> mapper) {
        List<R> mappedContent = page.getContent().stream()
                .map(mapper)
                .toList();

        return PagedResponse.<R>builder()
                .content(mappedContent)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        boolean isFirst = page == 0;
        boolean isLast = page >= totalPages - 1 || totalPages == 0;

        return PagedResponse.<T>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(isFirst)
                .last(isLast)
                .hasNext(!isLast)
                .hasPrevious(!isFirst)
                .build();
    }
}
