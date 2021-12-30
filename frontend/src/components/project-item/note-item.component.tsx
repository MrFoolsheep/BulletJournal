// note item component for note tree
// react import
import React from 'react';
import {connect} from 'react-redux';
import {useHistory} from 'react-router-dom';
// antd imports
import {
    BgColorsOutlined,
    CopyOutlined,
    DeleteTwoTone,
    FileTextOutlined,
    FormOutlined,
    MoreOutlined
} from '@ant-design/icons';
import {Avatar, message, Popconfirm, Popover, Tag, Tooltip} from 'antd';
// features import
import {deleteNote, updateColorSettingShown} from '../../features/notes/actions';
import {Note} from '../../features/notes/interface';
import {Label} from '../../features/label/interface';
import {setSelectedLabel} from '../../features/label/actions';
// modals import
import EditNote from '../modals/edit-note.component';
import MoveProjectItem from '../modals/move-project-item.component';
import ShareProjectItem from '../modals/share-project-item.component';
//  third party import
import moment from 'moment';
// assets import
import './project-item.styles.less';
import {ProjectItemUIType, ProjectType,} from '../../features/project/constants';
import {getIcon, getItemIcon,} from '../draggable-labels/draggable-label-list.component';
import {User} from '../../features/group/interface';
import {animation, IconFont, Item, Menu, MenuProvider} from "react-contexify";
import {theme as ContextMenuTheme} from "react-contexify/lib/utils/styles";
import CopyToClipboard from "react-copy-to-clipboard";
import 'react-contexify/dist/ReactContexify.min.css';
import {IState} from "../../store";
import {stringToRGB} from "../../utils/Util";

type ProjectProps = {
  readOnly: boolean;
  theme: string;
  showModal?: (user: User) => void;
  showOrderModal?: () => void;
};

type NoteProps = {
  inProject: boolean;
  setSelectedLabel: (label: Label) => void;
  updateColorSettingShown: (
    visible: boolean
  ) => void;
};

type NoteManageProps = {
  note: Note;
  inModal?: boolean;
  type: ProjectItemUIType;
  deleteNote: (noteId: number, type: ProjectItemUIType) => void;
};

const ManageNote: React.FC<NoteManageProps> = (props) => {
  const { note, deleteNote, inModal, type } = props;

  if (inModal === true) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column' }}>
        {note.editable && <Popconfirm
          title="Are you sure?"
          okText="Yes"
          cancelText="No"
          onConfirm={() => deleteNote(note.id, type)}
          className="group-setting"
          placement="bottom"
        >
          <div className="popover-control-item">
            <span>Delete</span>
            <DeleteTwoTone twoToneColor="#f5222d" />
          </div>
        </Popconfirm>}
      </div>
    );
  }

  return (
      <div style={{display: 'flex', flexDirection: 'column'}}>
          {note.editable && <EditNote note={note} mode="div"/>}
          {note.editable && <MoveProjectItem
              type={ProjectType.NOTE}
              projectItemId={note.id}
              mode="div"
          />}
          <ShareProjectItem
              type={ProjectType.NOTE}
              projectItemId={note.id}
              mode="div"
          />
          {note.deletable && <Popconfirm
              title="Are you sure?"
              okText="Yes"
              cancelText="No"
              onConfirm={() => deleteNote(note.id, type)}
              className="group-setting"
              placement="bottom"
          >
              <div className="popover-control-item">
                  <span>Delete</span>
                  <DeleteTwoTone twoToneColor="#f5222d"/>
              </div>
          </Popconfirm>}
      </div>
  );
};

const NoteItem: React.FC<ProjectProps & NoteProps & NoteManageProps> = (
  props
) => {
    const {
        note,
        type,
        theme,
        deleteNote,
        inModal,
        inProject,
        showModal,
        showOrderModal,
        readOnly,
        setSelectedLabel,
        updateColorSettingShown
    } = props;

  // hook history in router
  const history = useHistory();
  // jump to label searching page by label click
  const toLabelSearching = (label: Label) => {
    setSelectedLabel(label);
    history.push('/labels/search');
  };

  const getMore = () => {
    if (readOnly) {
      return null;
    }

    return (
      <Popover
        arrowPointAtCenter
        placement="rightTop"
        overlayStyle={{ width: '150px' }}
        content={
          <ManageNote
            note={note}
            deleteNote={deleteNote}
            inModal={inModal}
            type={type}
          />
        }
        trigger="click"
      >
        <span className="project-control-more">
          <MoreOutlined />
        </span>
      </Popover>
    );
  };

  const getAvatar = (user: User) => {
    if (!inProject || !showModal) return <span><Avatar src={user.avatar} size={24}/></span>;
    return (
        <span
            onClick={(e) => {
              e.stopPropagation();
              showModal(user);
            }}
        >
        <Avatar src={user.avatar} size={24} style={{cursor: 'pointer'}}/>
      </span>
    );
  };

  const getOrderIcon = () => {
    if (!inProject || !showOrderModal) return <FormOutlined />;
    return (
      <FormOutlined
        onClick={() => {
          showOrderModal();
        }}
      />
    );
  };

  const handleClick = () => {
    if (props.readOnly || note.shared) {
      // if readOnly, link to shared item page
      history.push(`/sharedItems/NOTE${note.id}`);
    } else {
      history.push(`/note/${note.id}`);
    }
  };

  const getProjectItemContentDiv = () => {
        return <div className="project-item-content">
            <a onClick={handleClick}>
                <h3 className="project-item-name">
                    <Tooltip title={note.owner.alias}>{getAvatar(note.owner)}</Tooltip>
                    &nbsp;{getItemIcon(note, <FileTextOutlined/>)}&nbsp;
                    {note.name}
                </h3>
            </a>
            <div className="project-item-subs">
                <div className="project-item-labels">
                    {note.labels &&
                    note.labels.map((label) => {
                        return (
                            <Tag
                                key={`label${label.id}`}
                                className="labels"
                                onClick={() => toLabelSearching(label)}
                                color={stringToRGB(label.value)}
                                style={{cursor: 'pointer', borderRadius: 10}}
                            >
                                        <span>
                                          {getIcon(label.icon)} &nbsp;
                                            {label.value}
                                        </span>
                            </Tag>
                        );
                    })}
                </div>
                <div className="project-item-time">
                    {note.createdAt && `Created ${moment(note.createdAt).fromNow()}`}
                </div>
            </div>
        </div>;
    }

    const handleClickChangeBgColor = (noteId: number) => {
      history.push(`/note/${noteId}`);
      updateColorSettingShown(true);
    }

    const getProjectItemContentWithMenu = () => {
        if (inModal === true) {
            return getProjectItemContentDiv()
        }

        return <>
            <MenuProvider id={`note${note.id}`}>
                {getProjectItemContentDiv()}
            </MenuProvider>

            <Menu id={`note${note.id}`} style={{background:bgColor}}
                  theme={theme === 'DARK' ? ContextMenuTheme.dark : ContextMenuTheme.light}
                  animation={animation.zoom}>
                <CopyToClipboard
                    text={`${note.name} ${window.location.origin.toString()}/#/note/${note.id}`}
                    onCopy={() => message.success('Link Copied to Clipboard')}
                >
                    <Item>
                        <IconFont style={{fontSize: '14px', paddingRight: '6px'}}><CopyOutlined/></IconFont>
                        <span>Copy Link Address</span>
                    </Item>
                </CopyToClipboard>
                {note.editable && <Item onClick={() => handleClickChangeBgColor(note.id)}>
                    <IconFont style={{fontSize: '14px', paddingRight: '6px'}}><BgColorsOutlined/></IconFont>
                    <span>Set Background Color</span>
                </Item>}
            </Menu>
        </>
    }

    const bgColorSetting = note.color ? JSON.parse(note.color) : undefined;
    const bgColor = bgColorSetting ? `rgba(${ bgColorSetting.r }, ${ bgColorSetting.g }, ${ bgColorSetting.b }, ${ bgColorSetting.a })` : undefined;

    return (
        <div className="project-item" style={{background: bgColor}}>
            {getProjectItemContentWithMenu()}
            <div className="project-control">
                <div style={{marginLeft: '5px'}}>
                    <Tooltip
                        title={
                            note.updatedAt && `Updated ${moment(note.updatedAt).fromNow()}`
                        }
                    >
                        {getOrderIcon()}
                    </Tooltip>
                </div>
                {getMore()}
            </div>
        </div>
  );
};

const mapStateToProps = (state: IState) => ({
    theme: state.myself.theme,
});

export default connect(mapStateToProps, { deleteNote, setSelectedLabel, updateColorSettingShown })(NoteItem);
