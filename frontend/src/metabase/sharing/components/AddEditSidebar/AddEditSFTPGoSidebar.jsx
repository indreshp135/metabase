import React, { useEffect, useState } from "react";
import PropTypes from "prop-types";
import _ from "underscore";
import { t } from "ttag";

import { PLUGIN_DASHBOARD_SUBSCRIPTION_PARAMETERS_SECTION_OVERRIDE } from "metabase/plugins";
import { dashboardPulseIsValid } from "metabase/lib/pulse";
import Input from "metabase/core/components/Input";
import Select from "metabase/core/components/Select";
import Icon from "metabase/components/Icon";
import Toggle from "metabase/core/components/Toggle";
import SchedulePicker from "metabase/containers/SchedulePicker";
import Sidebar from "metabase/dashboard/components/Sidebar";
import EmailAttachmentPicker from "metabase/sharing/components/EmailAttachmentPicker";
import SendTestPulse from "metabase/components/SendTestPulse";
import { SFTPApi } from "metabase/services";
import DeleteSubscriptionAction from "./DeleteSubscriptionAction";
import DefaultParametersSection from "./DefaultParametersSection";
import CaveatMessage from "./CaveatMessage";
import FolderTree from "./SFTPGoFile/FolderTree";
import Heading from "./Heading";
import { CHANNEL_NOUN_PLURAL } from "./constants";

function _AddEditSFTPGoSidebar({
  pulse,
  formInput,
  channel,
  channelSpec,
  users,
  parameters,
  defaultParametersById,
  dashboard,

  // form callbacks
  handleSave,
  onCancel,
  onChannelPropertyChange,
  onChannelScheduleChange,
  testPulse,
  toggleSkipIfEmpty,
  setPulse,
  handleArchive,
  setPulseParameters,
}) {
  const isValid = dashboardPulseIsValid(pulse, formInput.channels);
  const [explorer, setExplorer] = useState([]);

  useEffect(() => {
    SFTPApi.getFolders().then(res => {
      console.log(res);
      setExplorer(res);
    });
  }, []);

  return (
    <Sidebar
      closeIsDisabled={!isValid}
      onClose={handleSave}
      onCancel={onCancel}
    >
      <div className="pt4 px4 flex align-center">
        <Icon name="mail" className="mr1" size={21} />
        <Heading>{t`SFTP this dashboard`}</Heading>
      </div>
      <CaveatMessage />
      <div className="my2 px4">
        <div>
          <div className="text-bold mb1">{t`Subscription Name:`}</div>
          <Input
            type="text"
            fullWidth
            className="my1"
            value={channel.subscription_name}
            onChange={e =>
              onChannelPropertyChange("subscription_name", e.target.value)
            }
          />
        </div>
        <div
          className="my2 p2"
          style={{
            border: "1px solid #e0e0e0",
          }}
        >
          <div className="text-bold mb1">{t`Folder Path:`}</div>
          <Input
            fullWidth
            disabled
            className="mb1"
            value={channel.subscription_folder_path}
            onChange={e =>
              onChannelPropertyChange(
                "subscription_folder_path",
                e.target.value,
              )
            }
          />
          <FolderTree
            files={explorer}
            selectedFolderPath={channel.subscription_folder_path || ""}
            setSelectedFolderPath={path =>
              onChannelPropertyChange("subscription_folder_path", path)
            }
          />
        </div>
        <div className="my2">
          <div className="text-bold mb1">{t`Date Time Format:`}</div>
          <Select
            type="text"
            className="full"
            options={[
              { name: "empty", value: "" },
              { name: "YYYY-MM-DD HH:mm:ss", value: "YYYY-MM-DD HH:mm:ss" },
              { name: "YYYY-MM-DD", value: "YYYY-MM-DD" },
              { name: "HH:mm:ss", value: "HH:mm:ss" },
            ]}
            defaultValue={""}
            value={channel.subscription_date_time_format}
            onChange={e =>
              onChannelPropertyChange(
                "subscription_date_time_format",
                e.target.value,
              )
            }
          />
        </div>
        <SchedulePicker
          schedule={_.pick(
            channel,
            "schedule_day",
            "schedule_frame",
            "schedule_hour",
            "schedule_type",
          )}
          scheduleOptions={channelSpec.schedules}
          textBeforeInterval={t`Sent`}
          textBeforeSendTime={t`${
            CHANNEL_NOUN_PLURAL[channelSpec && channelSpec.type] || t`Messages`
          } will be sent at`}
          onScheduleChange={(newSchedule, changedProp) =>
            onChannelScheduleChange(newSchedule, changedProp)
          }
        />
        <div className="pt2 pb1">
          <SendTestPulse
            channel={channel}
            channelSpecs={formInput.channels}
            pulse={pulse}
            testPulse={testPulse}
            normalText={t`Upload File now`}
            successText={t`File Uploaded`}
            disabled={!isValid}
          />
        </div>
        {PLUGIN_DASHBOARD_SUBSCRIPTION_PARAMETERS_SECTION_OVERRIDE.Component ? (
          <PLUGIN_DASHBOARD_SUBSCRIPTION_PARAMETERS_SECTION_OVERRIDE.Component
            className="py3 mt2 border-top"
            parameters={parameters}
            dashboard={dashboard}
            pulse={pulse}
            setPulseParameters={setPulseParameters}
            defaultParametersById={defaultParametersById}
          />
        ) : (
          <DefaultParametersSection
            className="py3 mt2 border-top"
            parameters={parameters}
            defaultParametersById={defaultParametersById}
          />
        )}
        <div className="text-bold py3 flex justify-between align-center border-top">
          <Heading>{t`Don't send if there aren't results`}</Heading>
          <Toggle
            value={pulse.skip_if_empty || false}
            onChange={toggleSkipIfEmpty}
          />
        </div>
        <div className="text-bold py2 flex justify-between align-center border-top">
          <div className="flex align-center">
            <Heading>{t`Attach results`}</Heading>
            <Icon
              name="info"
              className="text-medium ml1"
              size={12}
              tooltip={t`Attachments can contain up to 2,000 rows of data.`}
            />
          </div>
        </div>
        <EmailAttachmentPicker
          cards={pulse.cards}
          pulse={pulse}
          setPulse={setPulse}
        />
        {pulse.id != null && (
          <DeleteSubscriptionAction
            pulse={pulse}
            handleArchive={handleArchive}
          />
        )}
      </div>
    </Sidebar>
  );
}

_AddEditSFTPGoSidebar.propTypes = {
  pulse: PropTypes.object,
  formInput: PropTypes.object.isRequired,
  channel: PropTypes.object.isRequired,
  channelSpec: PropTypes.object.isRequired,
  users: PropTypes.array,
  parameters: PropTypes.array.isRequired,
  defaultParametersById: PropTypes.object.isRequired,
  dashboard: PropTypes.object.isRequired,
  handleSave: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
  onChannelPropertyChange: PropTypes.func.isRequired,
  onChannelScheduleChange: PropTypes.func.isRequired,
  testPulse: PropTypes.func.isRequired,
  toggleSkipIfEmpty: PropTypes.func.isRequired,
  setPulse: PropTypes.func.isRequired,
  handleArchive: PropTypes.func.isRequired,
  setPulseParameters: PropTypes.func.isRequired,
};

export default _AddEditSFTPGoSidebar;
