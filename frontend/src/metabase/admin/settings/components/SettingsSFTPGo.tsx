import React, { useMemo } from "react";
import { jt, t } from "ttag";
import _ from "underscore";
import { connect } from "react-redux";
import FormProvider from "metabase/core/components/FormProvider";
import FormInput from "metabase/core/components/FormInput";
import FormSubmitButton from "metabase/core/components/FormSubmitButton";
import FormErrorMessage from "metabase/core/components/FormErrorMessage";
import Breadcrumbs from "metabase/components/Breadcrumbs";
import { SettingDefinition, Settings } from "metabase-types/api";
import Button from "metabase/core/components/Button";
import { SFTPGO_SCHEMA } from "../auth/constants";
import { updateSFTPGoSettings } from "../settings";
import {
  SFTPGoForm,
  SFTPGoFormCaption,
  SFTPGoFormHeader,
} from "./SettingsSFTOGo.styled";

const ENABLED_KEY = "sftpgo-auth-enabled";
const SFTPGO_CONNECTIONS = "sftpgo-auth-connections";

const BREADCRUMBS = [[t`SFTPGo`, "/admin/settings/SFTPGo"]];

export interface SFTPGoAuthFormProps {
  elements?: SettingDefinition[];
  settingValues?: Partial<Settings>;
  isEnabled: boolean;
  isSsoEnabled: boolean;
  onSubmit: (settingValues: Partial<Settings>) => void;
}

const SingleConnection = ({
  connection,
  index,
  settings,
}: {
  connection: any;
  index: number;
  settings: any;
}) => {
  return (
    <>
      <FormInput
        key={index}
        title={t`Connection name`}
        name={`${SFTPGO_CONNECTIONS}.${index}.name`}
        placeholder={t`Name of the connection`}
        defaultValue={connection.name}
        {...getFormFieldProps(settings[`${SFTPGO_CONNECTIONS}.${index}.name`])}
      />
      <FormInput
        name={`${SFTPGO_CONNECTIONS}.${index}.url`}
        title={t`SFTPGo URL`}
        placeholder={t`https://sftpgo.example.com`}
        description={<span>{t`The URL of the SFTPGo instance.`}</span>}
        nullable
        {...getFormFieldProps(settings[`${SFTPGO_CONNECTIONS}.${index}.url`])}
      />
      <FormInput
        name={`${SFTPGO_CONNECTIONS}.${index}.username`}
        title={t`SFTPGo Username`}
        description={<span>{t`The username used to connect to SFTPGo.`}</span>}
        placeholder={t`The username used to connect to SFTPGo.`}
        nullable
        {...getFormFieldProps(
          settings[`${SFTPGO_CONNECTIONS}.${index}.username`],
        )}
      />
      <FormInput
        type="password"
        name={`${SFTPGO_CONNECTIONS}.${index}.password`}
        title={t`SFTPGo Password`}
        description={<span>{t`The password used to connect to SFTPGo.`}</span>}
        placeholder={t`The password used to connect to SFTPGo.`}
        nullable
        {...getFormFieldProps(
          settings[`${SFTPGO_CONNECTIONS}.${index}.password`],
        )}
      />
    </>
  );
};

const SFTPGoAuthForm = ({
  elements = [],
  settingValues = {},
  onSubmit,
}: SFTPGoAuthFormProps): JSX.Element => {
  const settings = useMemo(() => {
    return _.indexBy(elements, "key");
  }, [elements]);
  const initialValues = useMemo(() => {
    const values = SFTPGO_SCHEMA.cast(settingValues, { stripUnknown: true });
    return { ...values, [ENABLED_KEY]: true };
  }, [settingValues]);

  const submit = (values: any) => {
    onSubmit(values);
  };

  const [connections, setConnections] = React.useState(
    initialValues[SFTPGO_CONNECTIONS] || [],
  );

  const addConnection = () => {
    setConnections([
      ...connections,
      { name: "", url: "", username: "", password: "" },
    ]);
  };

  const removeConnection = (index: number) => {
    setConnections(connections.filter((_: any, i: number) => i !== index));
    settings[SFTPGO_CONNECTIONS] = connections.filter(
      (_: any, i: number) => i !== index,
    );
  };

  return (
    <FormProvider
      initialValues={{
        [ENABLED_KEY]: true,
        [SFTPGO_CONNECTIONS]: connections,
      }}
      enableReinitialize
      validationSchema={SFTPGO_SCHEMA}
      validationContext={settings}
      onSubmit={submit}
    >
      {({ dirty }) => (
        <SFTPGoForm>
          <Breadcrumbs crumbs={BREADCRUMBS} />
          <SFTPGoFormHeader>{t`Connect With SFTPGo`}</SFTPGoFormHeader>
          <SFTPGoFormCaption>
            {t`Allows users with existing Metabase accounts to send subscriptions to SFTPGo Folder.`}
          </SFTPGoFormCaption>

          {connections.map((connection: any, index: number) => (
            <div
              key={index}
              className="p2 m2"
              style={{
                border: "1px solid #e5e5e5",
              }}
            >
              <SingleConnection
                connection={connection}
                index={index}
                settings={settings}
              />
              <Button
                color="red"
                fullWidth
                onClick={() => removeConnection(index)}
                type="button"
              >
                Remove Connection
              </Button>
            </div>
          ))}
          <div className="m2">
            <Button
              fullWidth
              type="button"
              color="blue"
              onClick={addConnection}
            >
              Add Connection
            </Button>
          </div>
          <FormSubmitButton
            title={
              settings[ENABLED_KEY].value ? t`Save changes` : t`Save and enable`
            }
            fullWidth
            primary
          />
          <FormErrorMessage />
        </SFTPGoForm>
      )}
    </FormProvider>
  );
};

const getFormFieldProps = (setting?: SettingDefinition) => {
  if (setting?.is_env_setting) {
    return { placeholder: t`Using ${setting.env_name}`, readOnly: true };
  }
};

const mapDispatchToProps = {
  onSubmit: updateSFTPGoSettings,
};

export default connect(null, mapDispatchToProps)(SFTPGoAuthForm);
