#! python3

import os
import json

MAIN_JSON = "stops.json"
MAIN_OUT = "stops_times.json"


def main():
    main_file = json.load(open(MAIN_JSON, 'r'))
    route_dict = {r["name"]: r for r in main_file}
    # Iterate through all files in folder
    for f_name in os.listdir():
        if f_name[-8:] != ".geojson":
            # If it's not a geojson file, ignore it
            continue

        route = json.load(open(f_name, 'r'))
        route_name = f_name[:-8]
        unmatched = []
        # Look through all the points
        for feature in route["features"]:
            if feature["geometry"]["type"] != "Point":
                # Ignore the one LineString
                continue

            name = feature["properties"]["Name"]
            desc = feature["properties"]["description"]

            # If the name matches one of the original stops
            # Save the time
            if name in route_dict:
                orig_route = route_dict[name]
                if "times" not in orig_route:
                    orig_route["times"] = {}
                orig_route["times"][route_name] = desc
                route_dict[name] = orig_route
            else:
                unmatched.append(feature)

        if len(unmatched):
            json.dump(unmatched,
                    open("unmatched/" + route_name + "_unmatched.json", 'w'),
                    indent=4)
        print("{} unmatched stops out of {} for {}"
              .format(len(unmatched),
                      len(route["features"]) - 1,
                      route_name))
    json.dump([r for r in route_dict.values()],
              open(MAIN_OUT, 'w'),
              indent=4)

if __name__ == '__main__':
    main()
