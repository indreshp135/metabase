import styled from "@emotion/styled";
import Form from "metabase/core/components/Form";
import { color } from "metabase/lib/colors";

export const SFTPGoForm = styled(Form)`
  margin: 0 1rem;
  max-width: 32.5rem;
`;

export const SFTPGoFormHeader = styled.h2`
  margin-top: 1rem;
`;

export const SFTPGoFormCaption = styled.p`
  color: ${color("text-medium")};
`;
