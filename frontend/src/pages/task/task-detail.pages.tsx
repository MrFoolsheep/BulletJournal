// page display contents of tasks
// react imports
import React, {useEffect, useState} from 'react';
// features
//actions
import {getReminderSettingString, getTaskBackgroundColor, Task, TaskStatus,} from '../../features/tasks/interface';
// antd imports
import {Avatar, BackTop, Button, Divider, message, Modal, Select, Tag, Tooltip} from 'antd';
import {AlertOutlined, ClockCircleOutlined, UnorderedListOutlined, EnvironmentOutlined} from '@ant-design/icons';
import './task-page.styles.less';
import 'braft-editor/dist/index.css';
import {ProjectItemUIType, ProjectType} from '../../features/project/constants';
import {convertToTextWithRRule} from '../../features/recurrence/actions';
import moment from 'moment';
import DraggableLabelsList from '../../components/draggable-labels/draggable-label-list.component';
import {Content} from '../../features/myBuJo/interface';
// components
import TaskContentList from '../../components/content/content-list.component';
//redux
import {IState} from '../../store';
import {connect} from 'react-redux';
//action
import {setTaskStatus, setContentsOrder} from '../../features/tasks/actions';
import {getDuration} from '../../components/project-item/task-item.component';
import {inPublicPage} from "../../index";
import {animation, IconFont, Item, Menu, MenuProvider} from "react-contexify";
import {theme as ContextMenuTheme} from "react-contexify/lib/utils/styles";
import CopyToClipboard from "react-copy-to-clipboard";
import {CopyOutlined} from "@ant-design/icons/lib";
import ContentDnd from "../../components/content-dnd/content.dnd.component";

const { Option } = Select;

export type TaskProps = {
  task: Task | undefined;
  theme: string;
  contents: Content[];
  contentEditable?: boolean;
  setTaskStatus: (taskId: number, taskStatus: TaskStatus, type: ProjectItemUIType) => void;
  isPublic?: boolean;
};

type TaskDetailProps = {
  labelEditable: boolean;
  taskOperation: Function;
  createContentElem: React.ReactNode;
  taskEditorElem: React.ReactNode;
  setContentsOrder: (
      taskId: number,
      order: number[]
  ) => void;
};

const TaskDetailPage: React.FC<TaskProps & TaskDetailProps> = (props) => {
  const {
    task,
    theme,
    labelEditable,
    taskOperation,
    createContentElem,
    taskEditorElem,
    contents,
    contentEditable,
    setTaskStatus,
    isPublic,
    setContentsOrder,
  } = props;
  const [inputStatus, setInputStatus] = useState('' as TaskStatus);
  const [reorderContentsVisible,setReorderContentsVisible] = useState(false);
  const [contentIdsOnOrder, setContentIdsOnOrder] = useState(contents.map(content => content.id))

    useEffect(() => {
        setContentIdsOnOrder(props.contents.map(content => content.id))
    },[props.contents])

    useEffect(() => {
    if (task) {
      setInputStatus(task.status);
    }
  }, [task]);

  const getLocation = (task: Task) => {
    if(!task.location){
        return null;
    }
    const taskLocation = `Location: ${task.location}`
    return (
        <Tooltip title={taskLocation}>
            <Tag icon={<EnvironmentOutlined />}>{task.location}</Tag>
        </Tooltip>
    );
  };

  const getDueDateTime = (task: Task) => {
    if (task.recurrenceRule) {
      let taskDue = convertToTextWithRRule(task.recurrenceRule);
      if (task.duration) {
        taskDue += `, last ${getDuration(task.duration)}`;
      }
      return (
        <Tooltip title={taskDue}>
          <Tag icon={<ClockCircleOutlined />}>{`Recurring: ${taskDue}`}</Tag>
        </Tooltip>
      );
    }

    if (!task.dueDate) {
      return null;
    }

    const targetTime = task.dueDate + ' ' + (task.dueTime ? task.dueTime : '00:00');
    const leftTime = moment.tz(targetTime, task.timezone);

    let dueDateTitle = leftTime.fromNow();
    if (task.duration) {
      dueDateTitle += `, last ${getDuration(task.duration)}`;
    }

    const taskDue = `${task.dueDate} ${task.dueTime ? task.dueTime : ''}`;
    return (
      <Tooltip title={`Due ${taskDue}, ${dueDateTitle}`}>
        <Tag icon={<ClockCircleOutlined />}>{taskDue}</Tag>
      </Tooltip>
    );
  };

  const getReminder = (task: Task) => {
    const text = getReminderSettingString(task.reminderSetting);
    if (text === 'No Reminder') return null;
    return (
      <Tooltip title={text}>
        <Tag icon={<AlertOutlined />}>{text.replace('Reminder: ', '')}</Tag>
      </Tooltip>
    );
  };

  const getTaskStatusDropdown = (task: Task) => {
    if (createContentElem === null || !task.editable) {
      return null;
    }
    if (inputStatus) {
      return (
        <Select
          style={{ width: '135px' }}
          value={inputStatus}
          onChange={(value: TaskStatus) => {
            setInputStatus(value);
            setTaskStatus(task.id, value, ProjectItemUIType.PAGE);
          }}
        >
          {Object.values(TaskStatus).map((s: string) => {
            return (
              <Option value={s} key={s}>
                {s.replace(/_/g, ' ')}
              </Option>
            );
          })}
        </Select>
      );
    }
    return (
      <Select
        style={{ width: '118px' }}
        placeholder="Set Status"
        onChange={(value: TaskStatus) => {
          setInputStatus(value);
          setTaskStatus(task.id, value, ProjectItemUIType.PAGE);
        }}
      >
        {Object.values(TaskStatus).map((s: string) => {
          return (
            <Option value={s} key={s}>
              {s.replace(/_/g, ' ')}
            </Option>
          );
        })}
      </Select>
    );
  };

    useEffect(() => {
        if (task) {
            document.title = task.name;
        }
    }, [task]);

    if (!task) return null;

    const getTaskStatisticsDiv = (task: Task) => {
        if (isPublic) {
            return null;
        }
        if (window.location.hash.startsWith('#/sampleTasks')) {
            return null;
        }
        return <div
            className="task-statistic-card"
            style={getTaskBackgroundColor(task.status, theme)}
        >
            {getDueDateTime(task)}
            {getReminder(task)}
            {getLocation(task)}
            {getTaskStatusDropdown(task)}
        </div>;
    };

    const setContentsOrderAndCloseModal = () => {
        setContentsOrder(task?.id, contentIdsOnOrder);
        setReorderContentsVisible(false);
    }

    const getReorderContextsModal = () => {
        return(
            <Modal
                destroyOnClose
                centered
                title='Drag to Reorder Contents'
                visible={reorderContentsVisible}
                okText='Confirm'
                onCancel={() => setReorderContentsVisible(false)}
                onOk={() => {
                    setContentsOrderAndCloseModal()
                }}
            >
                <div >
                    <ContentDnd contents={contents} setContentIdsOnOrder={setContentIdsOnOrder} projectItem={task}/>
                </div>
            </Modal>
        )
    }

    return (
    <div className={`task-page ${inPublicPage() ? 'publicPage' : ''} ${isPublic ? 'sharedItem' : ''}`}>
        <BackTop/>

        <Tooltip
            placement="top"
            title={`Created by ${task.owner.alias}`}
            className="task-avatar"
        >
        <span>
          <Avatar size="large" src={task.owner.avatar}/>
        </span>
        </Tooltip>
        <div className="task-title">
            <>
                <MenuProvider id={`task${task.id}`}>
                    <div className="label-and-name">{task.name}</div>
                </MenuProvider>
                <Menu id={`task${task.id}`}
                      theme={theme === 'DARK' ? ContextMenuTheme.dark : ContextMenuTheme.light}
                      animation={animation.zoom}>
                    <CopyToClipboard
                        text={`${task.name} ${window.location.origin.toString()}/#/task/${task.id}`}
                        onCopy={() => message.success('Link Copied to Clipboard')}
                    >
                        <Item>
                            <IconFont
                                style={{fontSize: '14px', paddingRight: '6px'}}><CopyOutlined/></IconFont>
                            <span>Copy Link Address</span>
                        </Item>
                    </CopyToClipboard>
                </Menu>
            </>
            {taskOperation()}
        </div>
        {task.editable && <div className="title-labels">
            <DraggableLabelsList
                mode={ProjectType.TODO}
                labels={task.labels}
                editable={labelEditable}
                itemId={task.id}
                itemShared={task.shared}
            />
        </div>}
        <Divider style={{marginTop: '5px', marginBottom: '0px'}}/>
        {getTaskStatisticsDiv(task)}
        <Divider style={{marginTop: '0px'}}/>
        <div className="task-content">
            <div className="content-list">
                {task.editable && contents.length > 1
                && !isPublic
                && <Tooltip title="Reorder Contents">
                    <Button
                        type="primary"
                        shape="circle"
                        icon={<UnorderedListOutlined />}
                        onClick={() => setReorderContentsVisible(true)}
                        style={{marginBottom: '5px'}}
                    />
                </Tooltip>}
                <TaskContentList
                    projectItem={task}
                    contents={contents}
                    contentEditable={contentEditable}
                />
            </div>
            {task.editable && getReorderContextsModal()}
            {createContentElem}
        </div>
        {taskEditorElem}
    </div>
  );
};

const mapStateToProps = (state: IState) => ({
  theme: state.myself.theme,
});

export default connect(mapStateToProps, {
  setTaskStatus,
  setContentsOrder,
})(TaskDetailPage);
