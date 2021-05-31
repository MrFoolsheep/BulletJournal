import React, { useEffect, useState } from "react";
import { Empty, Input, Tooltip } from "antd";
import {
    CheckCircleOutlined,
    ClockCircleOutlined,
    CloseCircleOutlined,
    QuestionCircleOutlined,
    SwapRightOutlined,
    DeleteOutlined,
    CopyOutlined,
    EditOutlined
} from "@ant-design/icons";

import './book-me.styles.less';
import { IState } from "../../store";
import { connect } from "react-redux";
import {
    getBookingLink,
    getBookingLinks,
    getBookMeUsername,
    updateBookMeUsername
} from "../../features/bookingLink/actions";
import { BookingLink } from "../../features/bookingLink/interface";
import BookMeDrawer, { getSlotSpan } from "../../components/book-me/book-me-drawer";
import { Project, ProjectsWithOwner } from "../../features/project/interface";
import { flattenOwnedProject, flattenSharedProject } from "../projects/projects.pages";
import { ProjectType } from "../../features/project/constants";
import getThemeColorVars from "../../utils/theme";
import { getTaskBackgroundColor } from "../../features/tasks/interface";

type ManageBookingProps = {
    ownedProjects: Project[];
    sharedProjects: ProjectsWithOwner[];
    myself: string;
    bookMeUsername: string;
    links: BookingLink[];
    getBookMeUsername: () => void;
    updateBookMeUsername: (name: string) => void;
    getBookingLinks: () => void;
    getBookingLink: (bookingLinkId: string, timezone?: string) => void;
}

const ManageBooking: React.FC<ManageBookingProps> = (
    {
        ownedProjects,
        sharedProjects,
        myself,
        bookMeUsername,
        links,
        getBookMeUsername,
        updateBookMeUsername,
        getBookingLinks,
        getBookingLink
    }
) => {
    const [name, setName] = useState(bookMeUsername ? bookMeUsername : myself);
    const [nameChanged, setNameChanged] = useState(false);
    const [cardIsClicked, setCardIsClicked] = useState(false);
    const [projects, setProjects] = useState<Project[]>([]);

    useEffect(() => {
        let updateProjects = [] as Project[];
        updateProjects = flattenOwnedProject(ownedProjects, updateProjects);
        updateProjects = flattenSharedProject(sharedProjects, updateProjects);
        setProjects(updateProjects.filter(p => !p.shared && p.projectType === ProjectType.TODO));
    }, [ownedProjects, sharedProjects]);

    useEffect(() => {
        if (!bookMeUsername) {
            // make GET request to retrieve
            getBookMeUsername();
        } else {
            setName(bookMeUsername);
        }
    }, [bookMeUsername]);

    useEffect(() => {
        getBookingLinks();
    }, []);

    const handleOnClick = (save: boolean) => {
        if (save) {
            // make PUT request to update bookMeUsername
            updateBookMeUsername(name);
        } else {
            setName(bookMeUsername);
        }
        setNameChanged(false);
    }

    return <div>
        <div className='book-me-name'>
            Name&nbsp;&nbsp;
            <Tooltip
                title="This is your name as it appears on your booking page and in email notifications.">
                <span className="question-icon">
                    <QuestionCircleOutlined />
                </span>
            </Tooltip>
            <Input className='book-me-name-input' value={name} onChange={(e) => {
                setNameChanged(true);
                setName(e.target.value);
            }} />
            <Tooltip placement="top" title='Save'>
                <CheckCircleOutlined
                    onClick={() => handleOnClick(true)}
                    style={{
                        marginLeft: '20px',
                        cursor: 'pointer',
                        color: '#00e600',
                        fontSize: 20,
                        visibility: nameChanged ? 'visible' : 'hidden'
                    }} />
            </Tooltip>
            <Tooltip placement="top" title='Cancel'>
                <CloseCircleOutlined
                    onClick={() => handleOnClick(false)}
                    style={{
                        marginLeft: '20px',
                        cursor: 'pointer',
                        color: '#ff0000',
                        fontSize: 20,
                        visibility: nameChanged ? 'visible' : 'hidden'
                    }} />
            </Tooltip>
        </div>
        <div>
            <BookMeDrawer bookMeDrawerVisible={cardIsClicked}
                setBookMeDrawerVisible={setCardIsClicked}
                projects={projects} />
        </div>
        <div className='link-cards'>
            {links.map(link => {
                const slotspan = getSlotSpan(link.slotSpan);
                const getBackgroundColor = (slotspan: string) => {
                    const tmp = slotspan.split(" ")[0];
                    switch (tmp) {
                        case "15":
                            return "#EFEFF1";
                        case "30":
                            return "#ECD4D4";
                        case "45":
                            return "#C9CBE1";
                        case "1":
                            return "#CCDBE2";
                        default:
                            return "#E5ADB5"
                    }
                }

                return <div className='link-card'
                    style={{ backgroundColor: getBackgroundColor(slotspan) }}
                    onClick={() => {
                        getBookingLink(link.id);
                        setCardIsClicked(true)
                    }}>
                    <div className="link-card-title">
                        <ClockCircleOutlined /> {slotspan} Booking
                    </div>
                    <div className="link-card-dates">
                        {link.startDate} <SwapRightOutlined /> {link.endDate}
                    </div>
                    <div className='link-card-operations'>
                        <Tooltip title="Clone">
                            <CopyOutlined />
                        </Tooltip>
                        <Tooltip title="Edit">
                            <EditOutlined />
                        </Tooltip>
                        <Tooltip title="Delete">
                            <DeleteOutlined />
                        </Tooltip>
                    </div>
                </div>
            })}
        </div>
    </div>
}

const mapStateToProps = (state: IState) => ({
    ownedProjects: state.project.owned,
    sharedProjects: state.project.shared,
    myself: state.myself.username,
    bookMeUsername: state.bookingReducer.bookMeUsername,
    links: state.bookingReducer.links
});

export default connect(mapStateToProps, {
    getBookMeUsername,
    updateBookMeUsername,
    getBookingLinks,
    getBookingLink
})(ManageBooking);