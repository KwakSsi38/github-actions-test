package back.domain.info.dto.response;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "응답 DTO는 서비스 계층에서 생성한 목록을 그대로 직렬화 대상으로 노출한다."
)
public class PageUpdateRequestResponse {
    private final List<UpdateRequestResponse> contents;
    private final long totalElements;
    private final int totalPages;
    private final int page;
    private final int size;

    public PageUpdateRequestResponse(Page<UpdateRequestResponse> page) {
        this.contents = page.getContent();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.page = page.getNumber();
        this.size = page.getSize();
    }
}
