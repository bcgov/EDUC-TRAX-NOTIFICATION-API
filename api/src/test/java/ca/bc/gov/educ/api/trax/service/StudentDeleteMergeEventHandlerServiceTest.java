package ca.bc.gov.educ.api.trax.service;

import ca.bc.gov.educ.api.trax.messaging.MessagePublisher;
import ca.bc.gov.educ.api.trax.properties.ApplicationProperties;
import ca.bc.gov.educ.api.trax.repository.EventRepository;
import ca.bc.gov.educ.api.trax.rest.RestUtils;
import ca.bc.gov.educ.api.trax.struct.CHESEmail;
import ca.bc.gov.educ.api.trax.struct.GradStudent;
import ca.bc.gov.educ.api.trax.struct.Student;
import ca.bc.gov.educ.api.trax.support.NatsMessageImpl;
import ca.bc.gov.educ.api.trax.util.JsonUtil;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import ca.bc.gov.educ.api.trax.exception.NotificationApiException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class StudentDeleteMergeEventHandlerServiceTest {

    @Mock
    private RestUtils restUtils;
    
    @Mock
    private CHESEmailService chesEmailService;
    
    @Mock
    private EventRepository eventRepository;
    
    @Mock
    private ApplicationProperties applicationProperties;
    
    @Mock
    private MessagePublisher messagePublisher;

    @Captor
    private ArgumentCaptor<CHESEmail> emailCaptor;

    private StudentDeleteMergeEventHandlerService service;

    @Before
    public void setUp() {
        service = new StudentDeleteMergeEventHandlerService(
            eventRepository, restUtils, chesEmailService, applicationProperties, messagePublisher);
    }

    @Test
    public void processStudentsMergeInfo_whenEitherStudentExists_shouldSendEmail() throws Exception {
        // Given
        val student = createMockStudent("student-1", "123456789");
        val trueStudent = createMockStudent("student-2", "123456788");
        
        // Mock one student found, one not found (OR logic)
        val gradStudent1 = createMockGradStudent("student-1", false);
        val gradStudent2 = createMockGradStudent("student-2", true);
        
        val natsMsgImpl1 = new NatsMessageImpl();
        natsMsgImpl1.setData(JsonUtil.getJsonBytesFromObject(gradStudent1));
        
        val natsMsgImpl2 = new NatsMessageImpl();
        natsMsgImpl2.setData(JsonUtil.getJsonBytesFromObject(gradStudent2));
        
        when(messagePublisher.requestMessage(anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture(natsMsgImpl1))
            .thenReturn(CompletableFuture.completedFuture(natsMsgImpl2));

        // When
        service.processStudentsMergeInfo(student, trueStudent);

        // Then
        verify(chesEmailService).sendEmail(any(), any(), any());
    }

    @Test
    public void processStudentsMergeInfo_whenBothStudentsExist_shouldSendEmail() throws Exception {
        // Given
        val student = createMockStudent("student-1", "123456789");
        val trueStudent = createMockStudent("student-2", "123456788");
        
        // Mock both students found (OR logic - both found = true)
        val gradStudent1 = createMockGradStudent("student-1", false);
        val gradStudent2 = createMockGradStudent("student-2", false);
        
        val natsMsgImpl1 = new NatsMessageImpl();
        natsMsgImpl1.setData(JsonUtil.getJsonBytesFromObject(gradStudent1));
        
        val natsMsgImpl2 = new NatsMessageImpl();
        natsMsgImpl2.setData(JsonUtil.getJsonBytesFromObject(gradStudent2));
        
        when(messagePublisher.requestMessage(anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture(natsMsgImpl1))
            .thenReturn(CompletableFuture.completedFuture(natsMsgImpl2));

        // When
        service.processStudentsMergeInfo(student, trueStudent);

        // Then
        verify(chesEmailService).sendEmail(any(), any(), any());
    }

    @Test
    public void processStudentsMergeInfo_whenNeitherStudentExists_shouldNotSendEmail() throws Exception {
        // Given
        val student = createMockStudent("student-1", "123456789");
        val trueStudent = createMockStudent("student-2", "123456788");
        
        // Mock both students not found (OR logic - both not found = false)
        val gradStudent1 = createMockGradStudent("student-1", true);
        val gradStudent2 = createMockGradStudent("student-2", true);
        
        val natsMsgImpl1 = new NatsMessageImpl();
        natsMsgImpl1.setData(JsonUtil.getJsonBytesFromObject(gradStudent1));
        
        val natsMsgImpl2 = new NatsMessageImpl();
        natsMsgImpl2.setData(JsonUtil.getJsonBytesFromObject(gradStudent2));
        
        when(messagePublisher.requestMessage(anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture(natsMsgImpl1))
            .thenReturn(CompletableFuture.completedFuture(natsMsgImpl2));

        // When
        service.processStudentsMergeInfo(student, trueStudent);

        // Then
        verify(chesEmailService, never()).sendEmail(any(), any(), any());
    }

    @Test
    public void processStudentsMergeInfo_whenApiError_shouldThrowRuntimeException() {
        // Given
        val student = createMockStudent("student-1", "123456789");
        val trueStudent = createMockStudent("student-2", "123456788");
        
        when(messagePublisher.requestMessage(anyString(), any()))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API Error")));

        // When & Then
        assertThatThrownBy(() -> service.processStudentsMergeInfo(student, trueStudent))
            .isInstanceOf(NotificationApiException.class)
            .hasMessageContaining("Failed to check students");
    }

    @Test
    public void prepareAndSendEmail_shouldCreateCorrectSubject() {
        // Given
        val pen = "123456789";
        val mergedToPen = "123456788";

        // When
        service.prepareAndSendEmail(pen, mergedToPen);

        // Then
        verify(chesEmailService).sendEmail(null, 
            "123456789 DEMERGED FROM 123456788 IN PEN, NOT DEMERGED IN GRAD", 
            "123456789 DEMERGED FROM 123456788 IN PEN, NOT DEMERGED IN GRAD");
    }

    @Test
    public void getEventType_shouldReturnDeleteMerge() {
        // When
        val eventType = service.getEventType();

        // Then
        assertThat(eventType).isEqualTo("DELETE_MERGE");
    }

    private Student createMockStudent(String studentId, String pen) {
        val student = new Student();
        student.setStudentID(studentId);
        student.setPen(pen);
        return student;
    }

    private GradStudent createMockGradStudent(String studentId, boolean notFound) {
        return notFound ? 
            GradStudent.builder().studentID(studentId).exception("not found").build() :
            GradStudent.builder().studentID(studentId).program("2018-EN").graduated("Y").exception(null).build();
    }
}
