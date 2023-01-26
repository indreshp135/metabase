import React, { useState } from "react";
import PropTypes from "prop-types";
import Icon from "metabase/components/Icon";

function Folder({ name, files, path, onSelect, selectedFolderPath }) {
  const [expand, setExpand] = useState(false);
  console.log("Selected", selectedFolderPath, "path", path, "files", files);
  return (
    <div
      style={{
        cursor: files.isFolder ? "pointer" : "default",
      }}
    >
      <div
        style={{
          display: "flex",
        }}
        className="py1"
      >
        {files.isFolder ? (
          <Icon
            name={expand ? "folder_open" : "folder"}
            size={20}
            style={{ opacity: 0.25 }}
          />
        ) : (
          <Icon name="file" size={16} />
        )}
        <span
          className="text-bold pl1"
          style={
            selectedFolderPath === path
              ? { color: "#509ee3" }
              : { color: "#000" }
          }
          onClick={() => {
            setExpand(!expand);
            onSelect(path);
          }}
        >
          {name}
        </span>
      </div>
      <div style={{ display: expand ? "block" : "none", paddingLeft: 15 }}>
        {files.items.map((file, index) => (
          <Folder
            key={index}
            name={file.name}
            files={file}
            selectedFolderPath={selectedFolderPath}
            path={`${path}/${file.name}`}
            onSelect={file.isFolder ? onSelect : null}
          />
        ))}
      </div>
    </div>
  );
}

Folder.propTypes = {
  name: PropTypes.string.isRequired,
  files: PropTypes.object.isRequired,
  path: PropTypes.string.isRequired,
  onSelect: PropTypes.func || null,
  selectedFolderPath: PropTypes.string.isRequired,
};

export default function FolderTree({
  files,
  setSelectedFolderPath,
  selectedFolderPath,
}) {
  const [expand, setExpand] = useState(false);
  console.log("Selected", selectedFolderPath, "files", files);

  return (
    <div
      className="p2"
      style={{
        border: "1px solid #e0e0e0",
      }}
    >
      <div
        style={{
          cursor: "pointer",
        }}
      >
        <div
          style={{
            display: "flex",
          }}
          className="py1"
        >
          <Icon
            name={expand ? "folder_open" : "folder"}
            size={20}
            style={{ opacity: 0.25 }}
          />
          <span
            className="text-bold pl1"
            style={
              selectedFolderPath === "."
                ? { color: "#509ee3" }
                : { color: "#000" }
            }
            onClick={() => {
              setExpand(!expand);
              setSelectedFolderPath(".");
            }}
          >
            Root
          </span>
        </div>
        <div style={{ display: expand ? "block" : "none", paddingLeft: 15 }}>
          {files.map((file, index) => (
            <Folder
              key={index}
              name={file.name}
              files={file}
              selectedFolderPath={selectedFolderPath}
              path={`./${file.name}`}
              onSelect={file.isFolder ? setSelectedFolderPath : null}
            />
          ))}
        </div>
      </div>
    </div>
  );
}

FolderTree.propTypes = {
  files: PropTypes.array.isRequired,
  setSelectedFolderPath: PropTypes.func.isRequired,
  selectedFolderPath: PropTypes.string.isRequired,
};
