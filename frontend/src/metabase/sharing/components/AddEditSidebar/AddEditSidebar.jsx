import { connect } from "react-redux";

import {
  getDefaultParametersById,
  getParameters,
} from "metabase/dashboard/selectors";
import _AddEditEmailSidebar from "./AddEditEmailSidebar";
import _AddEditSlackSidebar from "./AddEditSlackSidebar";
import _AddEditSFTPGoSidebar from "./AddEditSFTPGoSidebar";

const mapStateToProps = (state, props) => {
  return {
    parameters: getParameters(state, props),
    defaultParametersById: getDefaultParametersById(state, props),
  };
};

export const AddEditEmailSidebar =
  connect(mapStateToProps)(_AddEditEmailSidebar);
export const AddEditSlackSidebar =
  connect(mapStateToProps)(_AddEditSlackSidebar);
export const AddEditSFTPGoSidebar = connect(mapStateToProps)(
  _AddEditSFTPGoSidebar,
);
