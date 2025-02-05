// Add more constants here as needed
import { getBackendUrl } from "src/config";

export const AppBranding = "Stage";

const backendBaseUrl = getBackendUrl();

export const API_URL = backendBaseUrl;
export const MAX_RESULTS = 10;
export const DEFAULT_TIMEOUT = 5000;

// Debezium color constants
export const BrandColors = {
  green: "#a5c82d",
  lightGreen: "#7fc5a5",
  teal: "#58b2da",
};

// Application color constants
export const AppColors = {
  dark: "#292929",
  darkBlue: "#4f6c87",
  white: "#ffffff",
};

// Application constant strings
export const AppStrings = {
  source: "source",
  destination: "destination",
  pipeline: "pipeline",
};

// Connector schema
export const schema = {
  type: "object",
  properties: {
    name: { type: "string" },
    description: { type: "string" },
    type: { type: "string" },
    schema: { type: "string" },
    vault: { type: "array" },
    config: {
      type: "object",
      minProperties: 1,
    },
  },
  required: ["name", "type", "schema", "config"],
};

export const initialSchema = {
  type: "object",
  properties: {
    name: { type: "string" },
    description: { type: "string" },
    type: { type: "string" },
    schema: { type: "string" },
    vault: { type: "array" },
    config: {
      type: "object",
      // minProperties: 1,
    },
  },
  required: ["name", "type", "schema", "config"],
};
