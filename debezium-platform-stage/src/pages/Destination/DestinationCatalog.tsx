/* eslint-disable @typescript-eslint/no-explicit-any */
import {
  Content,
  ContentVariants,
  PageSection,
  SearchInput,
  ToggleGroup,
  ToggleGroupItem,
  Toolbar,
  ToolbarContent,
  ToolbarGroup,
  ToolbarItem,
} from "@patternfly/react-core";
import { ListIcon, ThIcon } from "@patternfly/react-icons";
import { useNavigate } from "react-router-dom";
import { CatalogGrid } from "@components/CatalogGrid";
import { FunctionComponent, useCallback, useState } from "react";
import { Catalog } from "src/apis/types";
import destinationCatalog from "../../__mocks__/data/DestinationCatalog.json";
import { debounce } from "lodash";
import _ from "lodash";

export interface ISinkProps {
  sampleProp?: string;
}

const DestinationCatalog: FunctionComponent<ISinkProps> = () => {
  const navigate = useNavigate();
  const [isSelected, setIsSelected] = useState<"list" | "grid">("grid");

  const [searchResult, setSearchResult] =
    useState<Catalog[]>(destinationCatalog);
  const [searchQuery, setSearchQuery] = useState<string>("");

  const onClear = () => {
    onSearch?.("");
  };

  const handleItemClick = (
    event:
      | MouseEvent
      | React.MouseEvent<any, MouseEvent>
      | React.KeyboardEvent<Element>
  ) => {
    const id = event.currentTarget.id;
    setIsSelected(id.split("-")[2]);
  };

  const debouncedSearch = useCallback(
    debounce((searchQuery: string) => {
      const filteredSource = _.filter(destinationCatalog, function (o) {
        return o.name.toLowerCase().includes(searchQuery.toLowerCase());
      });
      setSearchResult(filteredSource);
    }, 700),
    []
  );

  const onSearch = useCallback(
    (value: string) => {
      setSearchQuery(value);
      debouncedSearch(value);
    },
    [debouncedSearch]
  );

  const onDestinationSelection = (destinationId: string) => {
    navigate(`/destination/create_destination/${destinationId}`);
  };

  return (
    <>
      <PageSection isWidthLimited>
        <Content component="h1">Destination</Content>
        <Content component="p">
          Add a destination to capture the change data event. To start select a
          connector below once you select a connector you can configure it using
          form or smart editor option. You can also search the connector by its
          name or toggle the catalog between the list view or card view.
        </Content>
        <Toolbar
          id="toolbar-sticky"
          inset={{ default: "insetNone" }}
          className="custom-toolbar"
          isSticky
        >
          <ToolbarContent>
            <ToolbarItem>
              <SearchInput
                aria-label="Items  search input"
                placeholder="Search by name"
                value={searchQuery}
                onChange={(_event, value) => onSearch(value)}
                onClear={onClear}
              />
            </ToolbarItem>
            <ToolbarItem>
              <ToggleGroup aria-label="Display variant toggle group">
                <ToggleGroupItem
                  icon={<ThIcon />}
                  aria-label="grid"
                  buttonId="toggle-group-grid"
                  isSelected={isSelected === "grid"}
                  onChange={handleItemClick}
                />

                <ToggleGroupItem
                  icon={<ListIcon />}
                  aria-label="list"
                  buttonId="toggle-group-list"
                  isSelected={isSelected === "list"}
                  onChange={handleItemClick}
                />
              </ToggleGroup>
            </ToolbarItem>
            <ToolbarGroup align={{ default: "alignEnd" }}>
              <ToolbarItem>
                <Content component={ContentVariants.small}>
                  {searchResult.length} Items
                </Content>
              </ToolbarItem>
            </ToolbarGroup>
          </ToolbarContent>
        </Toolbar>
      </PageSection>
      <CatalogGrid
        onCardSelect={onDestinationSelection}
        catalogType="destination"
        displayType={isSelected}
        isAddButtonVisible={searchQuery.length === 0}
        searchResult={searchResult}
      />
    </>
  );
};

export { DestinationCatalog };
