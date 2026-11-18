package de.igsnanstein.bso;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InternshipCompanyControllerTest {
    private final JdbcTemplate db=mock(JdbcTemplate.class);
    private final InternshipCompanyController controller=new InternshipCompanyController(db);

    @Test void previewDoesNotWriteAndIdentifiesExistingCompanies(){
        when(db.queryForObject(anyString(),eq(Integer.class),any(Object.class)))
            .thenAnswer(call->"vorhandener betrieb".equals(call.getArgument(2))?1:0);
        var file=new MockMultipartFile("file","betriebe.csv","text/csv",
            "Betrieb\nVorhandener Betrieb\nNeuer Betrieb\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var preview=controller.preview(file);
        assertEquals(1,preview.added());
        assertEquals(1,preview.unchanged());
        assertEquals(0,preview.errors());
        verify(db,never()).update(anyString(),any(Object[].class));
    }

    @Test void duplicateNamesInSameFileAreRejected(){
        when(db.queryForObject(anyString(),eq(Integer.class),any(Object.class))).thenReturn(0);
        var file=new MockMultipartFile("file","betriebe.csv","text/csv",
            "Betrieb\nWerkstatt\nwerkstatt\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var preview=controller.preview(file);
        assertEquals(1,preview.errors());
    }

    @Test void missingHeaderIsRejected(){
        var file=new MockMultipartFile("file","betriebe.csv","text/csv",
            "FalscheSpalte\nWerkstatt\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
            ()->controller.preview(file));
    }
}
