import { substringAfter } from "/lib/storybook/utils";
import type { Request, Response } from "@enonic-types/core";

const HEADERS = {
  "Access-Control-Allow-Origin": "*",
};

export function all(req: Request): Response {
  const { url, scheme, host, port } = req;
  const path = substringAfter(url, "/storybook-preview");

  return {
    redirect: `${scheme}://${host}:${port}/webapp/${app.name}${path}`,
    headers: HEADERS,
  };
}
