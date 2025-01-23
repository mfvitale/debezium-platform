import {
  Card,
  CardBody,
  Bullseye,
  CardFooter,
  Content,
  Tooltip,
} from "@patternfly/react-core";
import { Handle, Position } from "reactflow";
import "./TransformLinkNode.css";

import { AutomationIcon, DataProcessorIcon } from "@patternfly/react-icons";
import { useData } from "../../appLayout/AppContext";
import { AppColors } from "@utils/constants";

type Predicate = {
  label: string;
  negate: boolean;
};

interface TransformLinkNodeProps {
  data: {
    label: string;
    sourcePosition: Position;
    targetPosition: Position;
    predicate?: Predicate;
  };
}

const TransformLinkNode: React.FC<TransformLinkNodeProps> = ({ data }) => {
  const { darkMode } = useData();

  return (
    <>
      <div className="transformationWrapper transformationGradient">
        <div
          className="transformationInner"
          style={
            darkMode
              ? {
                  background: AppColors.dark,
                }
              : {
                  backgroundColor: AppColors.white,
                  cursor: "unset",
                }
          }
        >
          <Handle type="target" id="smt-input" position={data.sourcePosition} />
          <Card
            ouiaId="BasicCard"
            isCompact
            isPlain
            style={{ position: "relative" }}
          >
            {data.predicate && (
              <div
                style={{
                  cursor: "pointer",
                  position: "absolute",
                  top: -4,
                  right: 5,
                  zIndex: 1,
                }}
              >
                <Tooltip content={data.predicate.label}>
                  <AutomationIcon style={{ fontSize: 10 }} />
                </Tooltip>
              </div>
            )}

            <CardBody
              style={{
                paddingTop: 8,
                paddingBottom: 2,
                paddingLeft: 10,
                paddingRight: 10,
              }}
            >
              <Bullseye>
                <div>
                  <DataProcessorIcon style={{ fontSize: 15 }} />
                </div>
              </Bullseye>
            </CardBody>
            <CardFooter
              style={{ paddingLeft: 10, paddingRight: 10, paddingBottom: 5 }}
            >
              <Content
                type="p"
                style={{
                  fontSize: "8px",
                  fontWeight: "bold",
                  maxWidth: "100px",
                  minWidth: "40px",
                  whiteSpace: "nowrap",
                  overflow: "hidden",
                  textOverflow: "ellipsis",
                  textAlign: "center",
                }}
              >
                {data.label}
              </Content>
            </CardFooter>
          </Card>

          <Handle
            type="source"
            id="smt-output"
            position={data.targetPosition}
          />
        </div>
      </div>
    </>
  );
};

export default TransformLinkNode;
