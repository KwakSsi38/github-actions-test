package back.global.infra.oci;

import tools.jackson.core.type.TypeReference;

/**
 * OCI Object Storage 파일 업로드/다운로드 서비스.
 *
 * <p>도메인에 무관하게 범용으로 사용할 수 있는 인프라 서비스입니다.
 */
public interface OciStorageService {

    // ── 다운로드 ──────────────────────────────────────────────────────────────

    /**
     * OCI Object Storage에서 파일을 다운로드합니다.
     *
     * @param objectName 오브젝트 경로
     * @return 파일 바이트 배열
     */
    byte[] download(String objectName);

    /**
     * OCI Object Storage에서 JSON 파일을 다운로드하여 단순 타입으로 역직렬화합니다.
     *
     * @param objectName 오브젝트 경로
     * @param clazz      대상 클래스
     * @return 역직렬화된 객체
     */
    <T> T downloadJson(String objectName, Class<T> clazz);

    /**
     * OCI Object Storage에서 JSON 파일을 다운로드하여 제네릭 컬렉션 타입으로 역직렬화합니다.
     *
     * <p>{@code List<Foo>} 등 타입 소거가 발생하는 제네릭 타입에 사용합니다.
     *
     * @param objectName    오브젝트 경로
     * @param typeReference Jackson TypeReference
     * @return 역직렬화된 객체
     */
    <T> T downloadJson(String objectName, TypeReference<T> typeReference);

    // ── 업로드 ────────────────────────────────────────────────────────────────

    /**
     * OCI Object Storage에 파일을 업로드합니다.
     *
     * @param objectName  오브젝트 경로
     * @param content     업로드할 바이트 배열
     * @param contentType MIME 타입
     */
    void upload(String objectName, byte[] content, String contentType);

    /**
     * OCI Object Storage에 객체를 JSON으로 직렬화하여 업로드합니다.
     *
     * @param objectName 오브젝트 경로
     * @param data       직렬화할 객체
     */
    void uploadJson(String objectName, Object data);

    // ── 편의 메서드 ───────────────────────────────────────────────────────────

    /**
     * 설정된 prefix를 포함한 전체 오브젝트 경로를 반환합니다.
     *
     * @param filename 파일명
     * @return prefix + filename
     */
    String objectName(String filename);
}