package teammates.ui.webapi;

import java.util.List;

import org.testng.annotations.Test;

import teammates.common.datatransfer.NotificationTargetUser;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.NotificationAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.util.Const;
import teammates.ui.output.NotificationData;
import teammates.ui.output.NotificationsData;

/**
 * SUT: {@link GetNotificationsAction}.
 */
public class GetNotificationsActionTest extends BaseActionTest<GetNotificationsAction> {

    // TODO: add tests for isfetchingall

    @Override
    String getActionUri() {
        return Const.ResourceURIs.NOTIFICATIONS;
    }

    @Override
    String getRequestMethod() {
        return GET;
    }

    @Test
    @Override
    protected void testExecute() {
        // See independent test cases
    }

    @Test
    @Override
    protected void testAccessControl() {
        InstructorAttributes instructor = typicalBundle.instructors.get("instructor1OfCourse1");
        loginAsInstructor(instructor.getGoogleId());
        ______TS("student notification not accessible to instructor");
        String[] requestParams = new String[] {
                Const.ParamsNames.NOTIFICATION_TARGET_USER, NotificationTargetUser.STUDENT.toString(),
                Const.ParamsNames.NOTIFICATION_IS_FETCHING_ALL, String.valueOf(true),
        };
        verifyCannotAccess(requestParams);

        ______TS("accessible to instructor");
        requestParams = new String[] {
                Const.ParamsNames.NOTIFICATION_TARGET_USER, NotificationTargetUser.INSTRUCTOR.toString(),
                Const.ParamsNames.NOTIFICATION_IS_FETCHING_ALL, String.valueOf(true),
        };
        verifyCanAccess(requestParams);

        ______TS("instructor notification not accessible to student");
        StudentAttributes studentAttributes = typicalBundle.students.get("student1InCourse1");
        loginAsStudent(studentAttributes.getGoogleId());
        requestParams = new String[] {
                Const.ParamsNames.NOTIFICATION_TARGET_USER, NotificationTargetUser.INSTRUCTOR.toString(),
                Const.ParamsNames.NOTIFICATION_IS_FETCHING_ALL, String.valueOf(true),
        };
        verifyCannotAccess(requestParams);

        ______TS("accessible to student");
        requestParams = new String[] {
                Const.ParamsNames.NOTIFICATION_TARGET_USER, NotificationTargetUser.STUDENT.toString(),
                Const.ParamsNames.NOTIFICATION_IS_FETCHING_ALL, String.valueOf(true),
        };
        verifyCanAccess(requestParams);

        ______TS("unknown target user");
        loginAsInstructor(instructor.getGoogleId());
        requestParams = new String[] {
                Const.ParamsNames.NOTIFICATION_TARGET_USER, "unknown",
                Const.ParamsNames.NOTIFICATION_IS_FETCHING_ALL, String.valueOf(true),
        };
        verifyHttpParameterFailureAcl(requestParams);

        ______TS("accessible to admin");
        loginAsAdmin();
        requestParams = new String[] {
                Const.ParamsNames.NOTIFICATION_TARGET_USER, null,
                Const.ParamsNames.NOTIFICATION_IS_FETCHING_ALL, String.valueOf(true),
        };
        verifyCanAccess(requestParams);
    }

    @Test
    public void testExecute_withValidUserTypeForNonAdmin_shouldReturnData() {
        ______TS("Request to fetch notification");
        InstructorAttributes instructor = typicalBundle.instructors.get("instructor1OfCourse1");
        loginAsInstructor(instructor.getGoogleId());
        NotificationAttributes notification = typicalBundle.notifications.get("notification5");
        int expectedNumberOfNotifications =
                logic.getActiveNotificationsByTargetUser(notification.getTargetUser()).size();

        String[] requestParams = new String[] {
                Const.ParamsNames.NOTIFICATION_TARGET_USER, NotificationTargetUser.INSTRUCTOR.toString(),
                Const.ParamsNames.NOTIFICATION_IS_FETCHING_ALL, String.valueOf(true),
        };

        GetNotificationsAction action = getAction(requestParams);
        JsonResult jsonResult = getJsonResult(action);

        NotificationsData output = (NotificationsData) jsonResult.getOutput();
        List<NotificationData> notifications = output.getNotifications();

        assertEquals(expectedNumberOfNotifications, notifications.size());

        NotificationData firstNotification = notifications.get(0);
        verifyNotificationEquals(notification, firstNotification);
    }

    @Test
    public void testExecute_withoutUserTypeForAdmin_shouldReturnAllNotifications() {
        ______TS("Admin request to fetch notification");
        int expectedNumberOfNotifications = typicalBundle.notifications.size();
        loginAsAdmin();
        NotificationAttributes notification = typicalBundle.notifications.get("notStartedNotification2");

        String[] requestParams = new String[] {
                Const.ParamsNames.NOTIFICATION_TARGET_USER, null,
                Const.ParamsNames.NOTIFICATION_IS_FETCHING_ALL, String.valueOf(true),
        };

        GetNotificationsAction action = getAction(requestParams);
        JsonResult jsonResult = getJsonResult(action);

        NotificationsData output = (NotificationsData) jsonResult.getOutput();
        List<NotificationData> notifications = output.getNotifications();

        assertEquals(expectedNumberOfNotifications,
                logic.getAllNotifications().size());
        assertEquals(expectedNumberOfNotifications, notifications.size());

        NotificationData firstNotification = notifications.get(0);
        verifyNotificationEquals(notification, firstNotification);
    }

    @Test
    public void testExecute_withoutUserTypeForNonAdmin_shouldFail() {
        ______TS("Request without user type for non admin");
        InstructorAttributes instructor = typicalBundle.instructors.get("instructor1OfCourse1");
        loginAsInstructor(instructor.getGoogleId());
        GetNotificationsAction action = getAction(Const.ParamsNames.NOTIFICATION_IS_FETCHING_ALL, String.valueOf(true));
        assertThrows(AssertionError.class, action::execute);
    }

    @Test
    public void testExecute_invalidUserType_shouldFail() {
        ______TS("Request without invalid user type");
        InstructorAttributes instructor = typicalBundle.instructors.get("instructor1OfCourse1");
        loginAsInstructor(instructor.getGoogleId());

        // when usertype is GENERAL
        verifyHttpParameterFailure(Const.ParamsNames.NOTIFICATION_TARGET_USER,
                NotificationTargetUser.GENERAL.toString(),
                Const.ParamsNames.NOTIFICATION_IS_FETCHING_ALL,
                String.valueOf(true));

        // when usertype is a random string
        verifyHttpParameterFailure(Const.ParamsNames.NOTIFICATION_TARGET_USER,
                "invalid string",
                Const.ParamsNames.NOTIFICATION_IS_FETCHING_ALL,
                String.valueOf(true));
    }

    private void verifyNotificationEquals(NotificationAttributes expected, NotificationData actual) {
        assertEquals(expected.getNotificationId(), actual.getNotificationId());
        assertEquals(expected.getStyle(), actual.getStyle());
        assertEquals(expected.getTargetUser(), actual.getTargetUser());
        assertEquals(expected.getTitle(), actual.getTitle());
        assertEquals(expected.getMessage(), actual.getMessage());
    }
}
