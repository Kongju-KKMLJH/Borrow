package kkmljh.borrow.common.storage;

import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FileStorageService — 이미지 업로드 저장")
class FileStorageServiceTest {

    @TempDir
    Path uploadDir;

    private FileStorageService service;

    @BeforeEach
    void setUp() {
        service = new FileStorageService(uploadDir.toString(), "/files");
    }

    private MultipartFile image(String filename, String contentType) {
        return new MockMultipartFile("files", filename, contentType, "bytes".getBytes());
    }

    @Test
    @DisplayName("저장하면 /files 접두어의 상대 URL을 돌려주고 실제 파일이 생긴다")
    void storeReturnsRelativeUrl() {
        String url = service.store(image("photo.jpg", "image/jpeg"));

        assertThat(url).startsWith("/files/").endsWith(".jpg");
        assertThat(Files.exists(uploadDir.resolve(url.substring("/files/".length())))).isTrue();
    }

    @Test
    @DisplayName("원본 파일명은 쓰지 않고 UUID 파일명으로 저장한다 (경로 조작·중복 방지)")
    void storeUsesUuidFilename() {
        String url = service.store(image("../../evil.jpg", "image/jpeg"));

        assertThat(url).doesNotContain("..").doesNotContain("evil");
        assertThat(url.substring("/files/".length())).matches("[0-9a-f]{32}\\.jpg");
    }

    @Test
    @DisplayName("같은 파일명을 두 번 올려도 서로 다른 URL이 나온다")
    void storeTwiceProducesDistinctUrls() {
        String first = service.store(image("photo.jpg", "image/jpeg"));
        String second = service.store(image("photo.jpg", "image/jpeg"));

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("여러 장을 올리면 입력 순서대로 URL 목록을 돌려준다")
    void storeAllKeepsOrder() {
        List<String> urls = service.storeAll(List.of(
                image("1.png", "image/png"),
                image("2.webp", "image/webp"),
                image("3.gif", "image/gif")));

        assertThat(urls).hasSize(3);
        assertThat(urls.get(0)).endsWith(".png");
        assertThat(urls.get(1)).endsWith(".webp");
        assertThat(urls.get(2)).endsWith(".gif");
    }

    @ParameterizedTest
    @CsvSource({
            "photo.JPEG, image/jpeg, .jpg",
            "photo.jpeg, image/jpeg, .jpg",
            "photo.PNG,  image/png,  .png",
            "photo.heic, image/heic, .heic",
            "photo.webp, image/webp, .webp"
    })
    @DisplayName("확장자는 소문자로 정규화하고 jpeg 는 jpg 로 통일한다")
    void normalizesExtension(String filename, String contentType, String expectedSuffix) {
        assertThat(service.store(image(filename, contentType))).endsWith(expectedSuffix);
    }

    @ParameterizedTest
    @CsvSource({
            "image/png,  .png",
            "image/webp, .webp",
            "image/gif,  .gif",
            "image/heif, .heif",
            "image/jpeg, .jpg",
            "image/bmp,  .jpg"
    })
    @DisplayName("허용 목록 밖 확장자는 content-type 으로 보정한다")
    void fallsBackToContentType(String contentType, String expectedSuffix) {
        assertThat(service.store(image("photo.txt", contentType))).endsWith(expectedSuffix);
    }

    @Test
    @DisplayName("확장자가 아예 없어도 content-type 으로 저장된다")
    void noExtension() {
        assertThat(service.store(image("photo", "image/png"))).endsWith(".png");
    }

    @Test
    @DisplayName("빈 파일은 EMPTY_FILE")
    void emptyFile() {
        MultipartFile empty = new MockMultipartFile("files", "photo.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> service.store(empty))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMPTY_FILE);
    }

    @Test
    @DisplayName("파일이 null 이면 EMPTY_FILE")
    void nullFile() {
        assertThatThrownBy(() -> service.store(null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMPTY_FILE);
    }

    @Test
    @DisplayName("파일 목록이 비었거나 null 이면 EMPTY_FILE")
    void emptyList() {
        assertThatThrownBy(() -> service.storeAll(List.of()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMPTY_FILE);

        assertThatThrownBy(() -> service.storeAll(null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMPTY_FILE);
    }

    @Test
    @DisplayName("이미지가 아닌 파일은 UNSUPPORTED_FILE_TYPE")
    void nonImage() {
        MultipartFile pdf = new MockMultipartFile("files", "doc.pdf", "application/pdf", "bytes".getBytes());

        assertThatThrownBy(() -> service.store(pdf))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    @DisplayName("content-type 이 없으면 UNSUPPORTED_FILE_TYPE")
    void missingContentType() {
        MultipartFile unknown = new MockMultipartFile("files", "photo.jpg", null, "bytes".getBytes());

        assertThatThrownBy(() -> service.store(unknown))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    @DisplayName("url-prefix 설정을 바꾸면 반환 URL 접두어도 바뀐다")
    void customUrlPrefix() {
        FileStorageService custom = new FileStorageService(uploadDir.toString(), "/static/img");

        assertThat(custom.store(image("photo.jpg", "image/jpeg"))).startsWith("/static/img/");
    }

    @Test
    @DisplayName("업로드 디렉터리가 없으면 생성한다")
    void createsUploadDirectory() {
        Path nested = uploadDir.resolve("nested/deeper");

        new FileStorageService(nested.toString(), "/files");

        assertThat(Files.isDirectory(nested)).isTrue();
    }
}
