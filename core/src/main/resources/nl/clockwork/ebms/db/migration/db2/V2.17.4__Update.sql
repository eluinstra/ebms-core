--
-- Copyright 2011 Clockwork
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

ALTER TABLE certificate_mapping RENAME COLUMN SOURCE TO "source";
RENAME TABLE url_mapping TO url_mapping_old;
CREATE TABLE url_mapping AS (SELECT SOURCE AS "source", destination FROM url_mapping_old) WITH DATA;
ALTER TABLE url_mapping ADD UNIQUE ("source");
DROP TABLE url_mapping_old;
