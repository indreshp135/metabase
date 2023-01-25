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
import { SFTPGO_SCHEMA } from "../auth/constants";
import { updateSFTPGoSettings } from "../settings";
import {
  SFTPGoForm,
  SFTPGoFormCaption,
  SFTPGoFormHeader,
} from "./SettingsSFTOGo.styled";

const ENABLED_KEY = "sftpgo-auth-enabled";
const SFTPGO_URL = "sftpgo-auth-url";
const SFTPGO_USERNAME = "sftpgo-auth-username";
const SFTPGO_PASSWORD = "sftpgo-auth-password";

const BREADCRUMBS = [[t`SFTPGo`, "/admin/settings/SFTPGo"]];

export interface SFTPGoAuthFormProps {
  elements?: SettingDefinition[];
  settingValues?: Partial<Settings>;
  isEnabled: boolean;
  isSsoEnabled: boolean;
  onSubmit: (settingValues: Partial<Settings>) => void;
}

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

  return (
    <FormProvider
      initialValues={initialValues}
      enableReinitialize
      validationSchema={SFTPGO_SCHEMA}
      validationContext={settings}
      onSubmit={onSubmit}
    >
      {({ dirty }) => (
        <SFTPGoForm disabled={!dirty}>
          <Breadcrumbs crumbs={BREADCRUMBS} />
          <SFTPGoFormHeader>{t`Connect With SFTPGo`}</SFTPGoFormHeader>
          <SFTPGoFormCaption>
            {t`Allows users with existing Metabase accounts to send subscriptions to SFTPGo Folder.`}
          </SFTPGoFormCaption>
          <FormInput
            name={SFTPGO_URL}
            title={t`SFTPGo URL`}
            placeholder={t`https://sftpgo.example.com`}
            {...getFormFieldProps(settings[SFTPGO_URL])}
          />
          <FormInput
            name={SFTPGO_USERNAME}
            title={t`SFTPGo Username`}
            description={
              <span>{t`The username used to connect to SFTPGo.`}</span>
            }
            placeholder={t`The username used to connect to SFTPGo.`}
            nullable
            {...getFormFieldProps(settings[SFTPGO_USERNAME])}
          />
          <FormInput
            type="password"
            name={SFTPGO_PASSWORD}
            title={t`SFTPGo Password`}
            description={
              <span>{t`The password used to connect to SFTPGo.`}</span>
            }
            placeholder={t`The password used to connect to SFTPGo.`}
            nullable
            {...getFormFieldProps(settings[SFTPGO_PASSWORD])}
          />
          <FormSubmitButton
            title={settings[ENABLED_KEY] ? t`Save changes` : t`Save and enable`}
            primary
            disabled={!dirty}
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
