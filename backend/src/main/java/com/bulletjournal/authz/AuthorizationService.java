package com.bulletjournal.authz;

import com.bulletjournal.contents.ContentType;
import com.bulletjournal.controller.models.ProjectSetting;
import com.bulletjournal.exceptions.UnAuthorizedException;
import com.bulletjournal.repository.ProjectSettingDaoJpa;
import com.bulletjournal.repository.models.CompletedTask;
import com.bulletjournal.repository.models.Group;
import com.bulletjournal.repository.models.Project;
import com.bulletjournal.repository.models.ProjectItemModel;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Component
public class AuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationService.class);

    public static String SUPER_USER = UUID.randomUUID().toString();

    public static Set<String> ADMINS = ImmutableSet.of(SUPER_USER);

    @Autowired
    @Lazy
    private ProjectSettingDaoJpa projectSettingDaoJpa;

    public <T extends ProjectItemModel> void validateRequesterInProjectGroup(String requester, T projectItem) {
        if (projectItem.isShared()) {
            return;
        }
        validateRequesterInProjectGroup(requester, projectItem.getProject());
    }

    public <T extends ProjectItemModel> void validateRequesterInProjectGroup(String requester, Project project) {
        if (ADMINS.contains(requester)) {
            return;
        }

        validateRequesterInGroup(requester, project.getGroup(), true);
    }

    public void validateRequesterInGroup(String requester, Group group, boolean acceptedUserOnly) {
        if (notInGroup(requester, group, acceptedUserOnly)) {
            throw new UnAuthorizedException("User " + requester + " not in Group "
                    + group.getName());
        }
    }

    private boolean notInGroup(String requester, Group group, boolean acceptedUserOnly) {
        return (acceptedUserOnly ? group.getAcceptedUsers() : group.getUsers())
                .stream().noneMatch(u -> Objects.equals(requester, u.getUser().getName()));
    }

    public void checkAuthorizedToOperateOnContent(
            String owner, String requester, ContentType contentType,
            Operation operation, Long contentId, Object... other)
            throws UnAuthorizedException {
        if (ADMINS.contains(requester)) {
            return;
        }

        switch (contentType) {
            case PROJECT:
                checkAuthorizedToOperateOnProject(owner, requester, operation, contentId);
                break;
            case GROUP:
                checkAuthorizedToOperateOnGroup(owner, requester, operation, contentId);
                break;
            case TASK:
            case NOTE:
            case TRANSACTION:
                checkAuthorizedToOperateOnProjectItem(owner, requester, operation, contentId, other);
                break;
            case CONTENT:
                checkAuthorizedToOperateOnProjectItemContent(owner, requester, operation, contentId, other);
                break;
            case LABEL:
                checkAuthorizedToOperateOnLabel(owner, requester, operation, contentId);
                break;
            case BANK_ACCOUNT:
                checkAuthorizedToOperateOnBankAccount(owner, requester, operation, contentId);
                break;
            default:
        }
    }

    /**
     * @param owner Content Owner
     */
    public boolean isContentEditable(
            String owner, String requester,
            String projectOwner, String projectItemOwner, ProjectItemModel projectItem) {
        if (projectItem.isShared()) {
            // contents of project item being shared specifically cannot be edited
            return false;
        }

        return isContentDeletable(owner, requester, projectOwner, projectItemOwner, projectItem);
    }

    /**
     * @param owner Content Owner
     */
    public boolean isContentDeletable(String owner, String requester,
                                      String projectOwner, String projectItemOwner, ProjectItemModel projectItem) {
        if (Objects.equals(owner, requester)
            || Objects.equals(projectOwner, requester)
            || Objects.equals(projectItemOwner, requester)) {

            return true;
        }

        ProjectSetting projectSetting = this.projectSettingDaoJpa.getProjectSetting(projectItem.getProject().getId());
        // projectSetting.isAllowEditContents() is true and user needs to be in project's group
        return projectSetting.isAllowEditContents() && !notInGroup(requester, projectItem.getProject().getGroup(), true);
    }

    /**
     * @param owner Project Item Owner
     */
    public boolean isProjectItemEditable(String owner, String requester, String projectOwner,
                                         Long contentId, ProjectItemModel projectItem) {
        if (projectItem.isShared()) {
            return false;
        }
        return isProjectItemEditableAndDeletable(owner, requester, projectOwner, contentId, projectItem);
    }

    /**
     * @param owner Project Item Owner
     */
    public boolean isProjectItemDeletable(String owner, String requester, String projectOwner,
                                          Long contentId, ProjectItemModel projectItem) {
        if (projectItem.isShared()) {
            return true;
        }

        return isProjectItemEditableAndDeletable(owner, requester, projectOwner, contentId, projectItem);
    }

    private boolean isProjectItemEditableAndDeletable(
            String owner, String requester, String projectOwner, Long contentId,
            ProjectItemModel projectItem) {
        if (projectItem.getId() < 0) {
            // Transaction that is created from BankAccountTransaction has negative id `-Math.abs(RAND.nextLong())`
            return false;
        }

        if (projectItem instanceof CompletedTask) {
            return true;
        }

        if (Objects.equals(owner, requester) || Objects.equals(projectOwner, requester)) {
            return true;
        }

        LOGGER.info("Project Item " + contentId + " is owner by " +
                owner + " and Project is owned by " + projectOwner + " while request is from " + requester);

        Project project = projectItem.getProject();
        ProjectSetting projectSetting = this.projectSettingDaoJpa.getProjectSetting(project.getId());
        // projectSetting.isAllowEditProjItems() is true and user needs to be in project's group
        return projectSetting.isAllowEditProjItems() && !notInGroup(requester, project.getGroup(), true);
    }

    private void checkAuthorizedToOperateOnBankAccount(
            String owner, String requester, Operation operation, Long contentId) {
        switch (operation) {
            case READ:
            case DELETE:
            case UPDATE:
                if (!Objects.equals(owner, requester)) {
                    throw new UnAuthorizedException("Bank account " + contentId + " is owner by " +
                            owner + " while request is from " + requester);
                }
                break;
            default:
        }
    }

    private void checkAuthorizedToOperateOnLabel(
            String owner, String requester, Operation operation, Long contentId) {
        switch (operation) {
            case DELETE:
            case UPDATE:
                if (!Objects.equals(owner, requester)) {
                    throw new UnAuthorizedException("Label " + contentId + " is owner by " +
                            owner + " while request is from " + requester);
                }
                break;
            default:
        }
    }

    private void checkAuthorizedToOperateOnGroup(
            String owner, String requester, Operation operation, Long contentId) {
        switch (operation) {
            case DELETE:
            case UPDATE:
                if (!Objects.equals(owner, requester)) {
                    throw new UnAuthorizedException("Group " + contentId + " is owner by " +
                            owner + " while request is from " + requester);
                }
                break;
            default:
        }
    }

    private void checkAuthorizedToOperateOnProject(
            String owner, String requester, Operation operation, Long contentId) {
        switch (operation) {
            case DELETE:
            case UPDATE:
                if (!Objects.equals(owner, requester)) {
                    throw new UnAuthorizedException("Project " + contentId + " is owner by " +
                            owner + " while request is from " + requester);
                }
                break;
            default:
        }
    }

    private void checkAuthorizedToOperateOnProjectItem(
            String owner, String requester, Operation operation, Long contentId, Object... other) {
        ProjectItemModel projectItem = (ProjectItemModel) other[0];
        Project project = projectItem.getProject();
        String projectOwner = project.getOwner();
        String errorMsg = "Project Item " + contentId + " is owner by " +
                owner + " and Project is owned by " + projectOwner +
                " and Project Item is owned by " + owner +
                " while request is from " + requester;
        switch (operation) {
            case DELETE:
                if (!isProjectItemDeletable(owner, requester, projectOwner, contentId, projectItem)) {
                    throw new UnAuthorizedException(errorMsg);
                }
                break;
            case UPDATE:
                if (!isProjectItemEditable(owner, requester, projectOwner, contentId, projectItem)) {
                    throw new UnAuthorizedException(errorMsg);
                }
                break;
        }
    }

    private void checkAuthorizedToOperateOnProjectItemContent(
            String owner, String requester, Operation operation, Long contentId, Object[] other) {
        String projectItemOwner = (String) other[0];
        String projectOwner = (String) other[1];
        ProjectItemModel projectItem = (ProjectItemModel) other[2];
        String errorMsg = "Project Item " + contentId + " is owner by " +
                owner + " and Project is owned by " + projectOwner +
                " and Project Item is owned by " + projectItemOwner +
                " while request is from " + requester;
        switch (operation) {
            case UPDATE:
                if (!isContentEditable(owner, requester, projectOwner, projectItemOwner, projectItem)) {
                    throw new UnAuthorizedException(errorMsg);
                }
                break;
            case DELETE:
                if (!isContentDeletable(owner, requester, projectOwner, projectItemOwner, projectItem)) {
                    throw new UnAuthorizedException(errorMsg);
                }
                break;
        }
    }
}
