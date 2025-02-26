/* eslint-disable @typescript-eslint/no-unused-vars */
import * as React from "react";
import {
  ActionGroup,
  Button,
  ButtonType,
  Card,
  CardBody,
  CardFooter,
  Content,
  DataList,
  DataListAction,
  DataListCell,
  DataListItemCells,
  Drawer,
  DrawerActions,
  DrawerCloseButton,
  DrawerContent,
  DrawerContentBody,
  DrawerHead,
  DrawerPanelBody,
  DrawerPanelContent,
  DrawerPanelDescription,
  Flex,
  FlexItem,
  Form,
  FormContextProvider,
  FormGroup,
  FormHelperText,
  FormSection,
  FormSelect,
  FormSelectOption,
  HelperText,
  HelperTextItem,
  PageSection,
  Skeleton,
  TextInput,
  Tooltip,
} from "@patternfly/react-core";
import { atom, useAtom } from "jotai";
import { useNavigate } from "react-router-dom";
import "./PipelineDesigner.css";
import {
  Destination,
  editPut,
  fetchDataTypeTwo,
  Pipeline,
  PipelineDestination,
  PipelineSource,
  Source,
  Transform,
} from "../../apis/apis";
import {
  DragDropSort,
  DragDropSortDragEndEvent,
  DraggableObject,
} from "@patternfly/react-drag-drop";
import {
  ArrowRightIcon,
  ExclamationCircleIcon,
  TrashIcon,
} from "@patternfly/react-icons";
import { ReactFlowProvider } from "reactflow";
import { useEffect, useState } from "react";
import { API_URL } from "@utils/constants";
import PipelineEditFlow from "@components/pipelineDesigner/PipelineEditFlow";
import ConnectorImage from "@components/ComponentImage";
import { CodeEditor, Language } from "@patternfly/react-code-editor";
import { useNotification } from "@appContext/AppNotificationContext";

// Define Jotai atoms
export const selectedSourceAtom = atom<Source | undefined>(undefined);
export const selectedDestinationAtom = atom<Destination | undefined>(undefined);
export const selectedTransformAtom = atom<Transform[]>([]);

const getItems = (
  selectedTransform: Transform[],
  onTempDelete: (id: string) => void
): DraggableObject[] =>
  selectedTransform.map((transform, idx) => ({
    id: `${idx}-${transform.id}=${transform.name}`,
    content: (
      <>
        <DataListItemCells
          dataListCells={[
            <DataListCell key={`item-${idx}`} style={{ alignSelf: "center" }}>
              <Content component="p">{transform.name}</Content>
            </DataListCell>,
          ]}
        />
        <DataListAction
          aria-labelledby="single-action-item1 single-action-action1"
          id="single-action-action1"
          aria-label="Actions"
        >
          <Tooltip content="Delete">
            <Button
              onClick={() =>
                onTempDelete(`${idx}-${transform.id}=${transform.name}`)
              }
              variant="plain"
              key="delete-action"
              icon={<TrashIcon />}
            />
          </Tooltip>
        </DataListAction>
      </>
    ),
  }));

type PipelineDesignerEditProps = {
  pipelineSource: PipelineSource;
  pipelineDestination: PipelineDestination;
  transforms: Transform[];
  name: string;
  desc: string;
  definedLogLevel: string;
  pipelineId: number;
};

const PipelineDesignerEdit: React.FunctionComponent<
  PipelineDesignerEditProps
> = ({
  pipelineSource,
  pipelineDestination,
  transforms,
  name,
  desc,
  definedLogLevel,
  pipelineId,
}) => {
  const navigate = useNavigate();

  const [items, setItems] = React.useState<DraggableObject[]>([]);
  const [editStep, setEditStep] = useState(0);

  const [editorSelected, setEditorSelected] = useState("form-editor");

  const { addNotification } = useNotification();

  // const [tempDeletedItems, setTempDeletedItems] = React.useState<Set<string>>(
  //   new Set()
  // );

  const [selectedTransform, setSelectedTransform] = useAtom(
    selectedTransformAtom
  );

  const [rearrangeTrigger, setRearrangeTrigger] = React.useState(false);

  const [isLoading, setIsLoading] = useState(false);

  const [isExpanded, setIsExpanded] = React.useState(false);
  const drawerRef = React.useRef<HTMLDivElement>();

  const [source, setSource] = useState<Source>();
  const [destination, setDestination] = useState<Destination>();
  const [isSourceFetchLoading, setIsSourceFetchLoading] =
    useState<boolean>(true);
  const [isDestinationFetchLoading, setIsDestinationFetchLoading] =
    useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchSource = async () => {
      setIsSourceFetchLoading(true);
      const response = await fetchDataTypeTwo<Source>(
        `${API_URL}/api/sources/${pipelineSource!.id}`
      );

      if (response.error) {
        setError(response.error);
      } else {
        setSource(response.data as Source);
      }
      setIsSourceFetchLoading(false);
    };

    if (pipelineSource?.id) {
      fetchSource();
    }
  }, [pipelineSource]);

  useEffect(() => {
    const fetchDestination = async () => {
      setIsDestinationFetchLoading(true);
      const response = await fetchDataTypeTwo<Destination>(
        `${API_URL}/api/destinations/${pipelineDestination!.id}`
      );

      if (response.error) {
        setError(response.error);
      } else {
        setDestination(response.data as Destination);
      }
      setIsDestinationFetchLoading(false);
    };

    if (pipelineDestination?.id) {
      fetchDestination();
    }
  }, [pipelineDestination]);

  useEffect(() => {
    setSelectedTransform(transforms);
  }, [setSelectedTransform, transforms]);

  // Handle temporary deletion of items
  const handleTempDelete = React.useCallback((id: string) => {
    // setTempDeletedItems((prev) => {
    //   const newSet = new Set(prev);
    //   newSet.add(id);
    //   return newSet;
    // });
    setItems((prevItems) => prevItems.filter((item) => item.id !== id));
  }, []);

  // Initialize items when selectedTransform changes
  useEffect(() => {
    if (selectedTransform.length > 0) {
      setItems(getItems(selectedTransform, handleTempDelete));
      // setTempDeletedItems(new Set()); // Reset deleted items when transforms change
    }
  }, [selectedTransform, handleTempDelete]);

  const onExpand = () => {
    drawerRef.current && drawerRef.current.focus();
  };

  const onToggleDrawer = () => {
    if (isExpanded) {
      // Reset temporary state when closing drawer without applying
      setItems(getItems(selectedTransform, handleTempDelete));
      // setTempDeletedItems(new Set());
    }
    setIsExpanded(!isExpanded);
  };

  const onCloseClick = () => {
    // Reset temporary state when closing drawer without applying
    setItems(getItems(selectedTransform, handleTempDelete));
    // setTempDeletedItems(new Set());
    setIsExpanded(false);
  };

  const updateSelectedTransform = React.useCallback(
    (transform: Transform) => {
      setSelectedTransform((prevTransforms) => [...prevTransforms, transform]);
    },
    [setSelectedTransform]
  );

  const navigateTo = (url: string) => {
    navigate(url);
  };

  // Function to handle rearrange and delete apply button click
  const handleRearrangeClick = () => {
    const updatedTransforms = items.map((item) => {
      const [indexId, name] = String(item.id).split("=");
      const [_, id] = indexId.split("-");
      return { id: Number(id), name };
    });

    setSelectedTransform(...[updatedTransforms]);
    setRearrangeTrigger((prev) => !prev);
    onToggleDrawer();
  };

  const reArrangeTransform = (
    _event: DragDropSortDragEndEvent,
    newItems: DraggableObject[],
    _oldIndex?: number | undefined,
    _newIndex?: number | undefined
  ) => {
    setItems(newItems);
  };

  const editPipeline = async (values: Record<string, string>) => {
    setIsLoading(true);
    const payload = {
      description: values["descriptions"],
      logLevel: logLevel,
      name: values["pipeline-name"],
      transforms: selectedTransform,
    };

    const response = await editPut(
      `${API_URL}/api/pipelines/${pipelineId}`,
      payload
    );
    setIsLoading(false);

    if (response.error) {
      addNotification(
        "danger",
        `Edit failed`,
        `Failed to edit ${(response.data as Pipeline).name}: ${response.error}`
      );
    } else {
      addNotification(
        "success",
        `Edit successful`,
        `Pipeline "${(response.data as Pipeline).name}" edited successfully.`
      );
      navigateTo("/pipeline");
    }
  };

  const handleEditPipeline = async (values: Record<string, string>) => {
    await editPipeline(values);
  };

  const [logLevel, setLogLevel] = useState("");

  useEffect(() => {
    setLogLevel(definedLogLevel);
  }, [definedLogLevel]);

  const onChange = (
    _event: React.FormEvent<HTMLSelectElement>,
    value: string
  ) => {
    setLogLevel(value);
  };

  const options = [
    { value: "", label: "Select log level", disabled: true },
    { value: "OFF", label: "OFF", disabled: false },
    { value: "FATAL", label: "FATAL", disabled: false },
    { value: "ERROR", label: "ERROR", disabled: false },
    { value: "WARN", label: "WARN", disabled: false },
    { value: "INFO", label: "INFO", disabled: false },
    { value: "DEBUG", label: "DEBUG", disabled: false },
    { value: "TRACE", label: "TRACE", disabled: false },
    { value: "ALL", label: "ALL", disabled: false },
  ];

  const panelContent = (
    <DrawerPanelContent>
      <DrawerHead>
        <Content component="h4" tabIndex={isExpanded ? 0 : -1}>
          Transform list
        </Content>
        <DrawerActions>
          <DrawerCloseButton onClick={onCloseClick} />
        </DrawerActions>
      </DrawerHead>
      <DrawerPanelDescription>
        List of transform to be applied to pipeline you can delete or rearrange
        them on specific order for them to be applied.
      </DrawerPanelDescription>
      <DrawerPanelBody style={{ display: "inline-block" }}>
        {selectedTransform.length === 0 ? (
          <>No transform configured</>
        ) : (
          <>
            <DragDropSort
              items={items}
              onDrop={reArrangeTransform}
              variant="DataList"
              overlayProps={{ isCompact: true }}
            >
              <DataList aria-label="draggable data list example" isCompact />
            </DragDropSort>
            <Button
              variant="primary"
              style={{ marginTop: "15px" }}
              onClick={handleRearrangeClick}
            >
              Apply
            </Button>
          </>
        )}
      </DrawerPanelBody>
    </DrawerPanelContent>
  );

  return (
    <>
      {editStep === 0 ? (
        <Drawer isExpanded={isExpanded} onExpand={onExpand}>
          <DrawerContent panelContent={panelContent}>
            <DrawerContentBody>
              <PageSection isFilled padding={{ default: "noPadding" }}>
                <Card isFullHeight>
                  <CardBody isFilled style={{ height: "100%", width: "100%" }}>
                    <ReactFlowProvider>
                      <PipelineEditFlow
                        sourceName={source?.name || ""}
                        sourceType={source?.type || ""}
                        selectedTransform={selectedTransform}
                        destinationName={destination?.name || ""}
                        destinationType={destination?.type || ""}
                        updateSelectedTransform={updateSelectedTransform}
                        openTransformDrawer={onToggleDrawer}
                      />
                    </ReactFlowProvider>
                  </CardBody>

                  <CardFooter
                    className="custom-card-footer"
                    style={{ padding: 0 }}
                  >
                    <ActionGroup className="create_pipeline-footer">
                      <Button
                        variant="primary"
                        onClick={() => {
                          console.log("Save and next", selectedTransform);
                          setEditStep((prevStep) => prevStep + 1);
                        }}
                      >
                        Save and next
                      </Button>
                      <Button variant="link" onClick={() => {}}>
                        Cancel
                      </Button>
                    </ActionGroup>
                  </CardFooter>
                </Card>
              </PageSection>
            </DrawerContentBody>
          </DrawerContent>
        </Drawer>
      ) : (
        <FormContextProvider
          initialValues={{
            "pipeline-name": name || "",
            descriptions: desc || "",
          }}
        >
          {({ setValue, getValue, setError, values, errors }) => (
            <>
              <PageSection
                isWidthLimited={editorSelected === "form-editor"}
                isCenterAligned
                isFilled
                style={{
                  paddingTop: "0",
                  paddingLeft: "0",
                  paddingRight: "0",
                  height: "100%",
                }}
                className="pipeline-page-section"
              >
                {editorSelected === "form-editor" ? (
                  <Card className="pipeline-card-body">
                    <CardBody isFilled>
                      <Form isWidthLimited>
                        <FormGroup
                          label="Pipeline flow"
                          isRequired
                          fieldId="pipeline-flow-field"
                        >
                          <Flex alignItems={{ default: "alignItemsCenter" }}>
                            {isSourceFetchLoading ? (
                              <>
                                <FlexItem spacer={{ default: "spacerMd" }}>
                                  <Skeleton
                                    shape="circle"
                                    width="15%"
                                    screenreaderText="Loading source"
                                  />
                                </FlexItem>
                                <FlexItem>
                                  <Skeleton
                                    shape="circle"
                                    width="15%"
                                    screenreaderText="Loading source"
                                  />
                                </FlexItem>
                              </>
                            ) : (
                              <>
                                <FlexItem spacer={{ default: "spacerMd" }}>
                                  <ConnectorImage
                                    connectorType={source?.type || ""}
                                    size={30}
                                  />
                                </FlexItem>
                                <FlexItem spacer={{ default: "spacerMd" }}>
                                  {source?.name}
                                </FlexItem>
                              </>
                            )}

                            <FlexItem spacer={{ default: "spacerMd" }}>
                              <ArrowRightIcon />
                            </FlexItem>
                            {isDestinationFetchLoading ? (
                              <>
                                <FlexItem spacer={{ default: "spacerMd" }}>
                                  <Skeleton
                                    shape="circle"
                                    width="15%"
                                    screenreaderText="Loading destination"
                                  />
                                </FlexItem>
                                <FlexItem>
                                  <Skeleton
                                    shape="circle"
                                    width="15%"
                                    screenreaderText="Loading destination"
                                  />
                                </FlexItem>
                              </>
                            ) : (
                              <>
                                <FlexItem spacer={{ default: "spacerMd" }}>
                                  <ConnectorImage
                                    connectorType={destination?.type || ""}
                                    size={30}
                                  />
                                </FlexItem>
                                <FlexItem spacer={{ default: "spacerMd" }}>
                                  {destination?.name}
                                </FlexItem>
                              </>
                            )}
                          </Flex>
                        </FormGroup>
                        <FormGroup
                          label="Pipeline name"
                          isRequired
                          fieldId="pipeline-name-field"
                        >
                          <TextInput
                            id="pipeline-name"
                            aria-label="pipeline name"
                            onChange={(_event, value) => {
                              setValue("pipeline-name", value);
                              setError("pipeline-name", undefined);
                            }}
                            value={getValue("pipeline-name")}
                            validated={
                              errors["pipeline-name"] ? "error" : "default"
                            }
                          />
                          <FormHelperText>
                            <HelperText>
                              <HelperTextItem
                                variant={
                                  errors["pipeline-name"] ? "error" : "default"
                                }
                                {...(errors["pipeline-name"] && {
                                  icon: <ExclamationCircleIcon />,
                                })}
                              >
                                {errors["pipeline-name"]}
                              </HelperTextItem>
                            </HelperText>
                          </FormHelperText>
                        </FormGroup>
                        <FormGroup
                          label="Description"
                          fieldId="descriptions-field"
                        >
                          <TextInput
                            id="descriptions"
                            aria-label="Pipeline description"
                            onChange={(_event, value) =>
                              setValue("descriptions", value)
                            }
                            value={getValue("descriptions")}
                          />
                          <FormHelperText>
                            <HelperText>
                              <HelperTextItem>
                                Add a one liner to describe your pipeline or
                                what you plan to capture.
                              </HelperTextItem>
                            </HelperText>
                          </FormHelperText>
                        </FormGroup>
                        <FormSection
                          title="Configuration properties"
                          titleElement="h2"
                          className="custom-form-group"
                        >
                          <FormGroup
                            label="Log level"
                            isRequired
                            fieldId="logLevel-field"
                          >
                            <FormSelect
                              value={logLevel}
                              onChange={onChange}
                              aria-label="FormSelect Input"
                              ouiaId="BasicFormSelect"
                            >
                              {options.map((option, index) => (
                                <FormSelectOption
                                  isDisabled={option.disabled}
                                  key={index}
                                  value={option.value}
                                  label={option.label}
                                />
                              ))}
                            </FormSelect>
                          </FormGroup>
                        </FormSection>
                      </Form>
                    </CardBody>
                  </Card>
                ) : (
                  <CodeEditor
                    isUploadEnabled
                    isDownloadEnabled
                    isCopyEnabled
                    isLanguageLabelVisible
                    isMinimapVisible
                    language={Language.yaml}
                    height="450px"
                  />
                )}
              </PageSection>
              <PageSection className="pf-m-sticky-bottom" isFilled={false}>
                <ActionGroup className="create_pipeline-footer">
                  <Button
                    variant="primary"
                    isLoading={isLoading}
                    isDisabled={isLoading}
                    type={ButtonType.submit}
                    onClick={(e) => {
                      e.preventDefault();

                      if (!values["pipeline-name"]) {
                        setError("pipeline-name", "Pipeline name is required.");
                      } else {
                        handleEditPipeline(values);
                      }
                    }}
                  >
                    Update pipeline
                  </Button>
                  <Button
                    variant="link"
                    onClick={() => setEditStep((prevStep) => prevStep - 1)}
                  >
                    Back
                  </Button>
                </ActionGroup>
              </PageSection>
            </>
          )}
        </FormContextProvider>
      )}
    </>
  );
};

export { PipelineDesignerEdit };
