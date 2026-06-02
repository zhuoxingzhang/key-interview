from __future__ import annotations

import json
import re
import time
from abc import ABC, abstractmethod
from collections import defaultdict
from random import Random
from typing import Collection, List, Optional, Set
from Schema_Desc_DB import schema_desc_db
from Schema_Info_DB import schema_info_db
from Key import Key


class Utils:
    @staticmethod
    def printKeys(keys: Collection[Key]) -> None:
        print("###########")
        for key in keys:
            print(key)
        print("###########\n")

    @staticmethod
    def refineToMinimalKeys(keys: List[Key]) -> Set[Key]:
        minimal_keys: Set[Key] = set()
        keys = list(keys)
        keys.sort(key=lambda k: k.size())

        for key in keys:
            is_minimal = True
            for existing in minimal_keys:
                if key.contains(existing):
                    is_minimal = False
                    break
            if is_minimal:
                minimal_keys.add(key)

        return minimal_keys


class KeyOracle(ABC):
    """
    Oracle interface for deciding whether a candidate attribute set is a key.

    Return value semantics:
      - True  -> candidateKey is a key (including superkey)
      - False -> candidateKey is NOT a key
    """

    @abstractmethod
    def ask_is_key(
            self,
            schema: List[str],
            schema_name: str,
            candidate_key: Key,
            print_output: bool = False,
    ) -> bool:
        raise NotImplementedError


class KeyInterviewOracle(KeyOracle):

    def __init__(
            self,
            model_name_or_path: str,
            device_map: str = "auto",
            torch_dtype: str = "auto",
            max_new_tokens: int = 8,
    ):
        try:
            import torch
            from transformers import AutoModelForCausalLM, AutoTokenizer
        except ImportError as e:
            raise ImportError(
                "QwenHFKeyOracle requires `transformers` and `torch` to be installed."
            ) from e

        self._torch = torch
        self.max_new_tokens = max_new_tokens
        self.tokenizer = AutoTokenizer.from_pretrained(model_name_or_path, trust_remote_code=True)
        self.model = AutoModelForCausalLM.from_pretrained(
            model_name_or_path,
            device_map=device_map,
            torch_dtype=torch_dtype,
            trust_remote_code=True,
        )

    def ask_is_key(
            self,
            schema: List[str],
            schema_name: str,
            candidate_key: Key,
            print_output: bool = False,
    ) -> bool:
        prompt = self.build_prompt(schema, candidate_key, schema_name)
        print("\n" + "#" * 20)
        print("[LLM prompt]")
        print(prompt)
        print("#" * 20 + "\n")
        raw = self._generate(prompt)
        parsed = self.parse_json_answer(raw)  # Yes -> True | No -> False
        if parsed is None:
            print("Warning! Output could not be parsed as Yes/No. By default, we set it to No.")
            parsed = False
            # raise ValueError(
            #     "Qwen output could not be parsed as Yes/No. "
            #     f"Raw output: {raw!r}"
            # )

        is_key = parsed  # Yes -> True

        if print_output:
            print(f"[LLM raw output] {raw}\n")
            print(f"[LLM parsed answer] {parsed}\n")

        return is_key

    def ask_all_minimal_keys(
            self,
            schema: List[str],
            schema_name: str,
            print_output: bool = False,
    ) -> Optional[List[Key]]:
        prompt = self.build_prompt_4_all_min_keys(schema, schema_name)

        print("\n" + "#" * 20)
        print("[LLM prompt - ALL MINIMAL KEYS]")
        print(prompt)
        print("#" * 20 + "\n")

        raw = self._generate(prompt)

        parsed = self.parse_minimal_keys(raw)

        if parsed is None:
            raise ValueError(f"Failed to parse minimal keys. Raw output: {raw}")

        if print_output:
            print(f"[LLM raw output] {raw}\n")
            print(f"[LLM parsed answer] {parsed}\n")

        return parsed

    def ask_prime_attributes(
            self,
            schema: List[str],
            schema_name: str,
            print_output: bool = False,
    ) -> Optional[List[str]]:

        prompt = self.build_prompt_4_prime_attributes(
            schema,
            schema_name
        )

        print("\n" + "#" * 20)
        print("[LLM prompt - PRIME ATTRIBUTES]")
        print(prompt)
        print("#" * 20 + "\n")

        raw = self._generate(prompt)

        parsed = self.parse_prime_attributes(raw)

        if parsed is None:
            raise ValueError(f"Failed to parse prime attributes. Raw output: {raw}")

        if print_output:
            print(f"[LLM raw output] {raw}\n")
            print(f"[LLM parsed answer] {parsed}\n")

        return parsed

    def build_prompt(self, schema: List[str], candidate_key: Key, schema_name: str) -> str:
        cand_attrs = candidate_key.getAttributes()
        table_str = Interview.renderSampleRelation(schema, cand_attrs)
        schema_str = ", ".join(schema)
        cand_str = ", ".join(cand_attrs) if cand_attrs else "<empty set>"
        schema_desc = schema_desc_db(schema_name)

        return (
            "You are an expert in relational database schema design.\n\n"

            f"Relational schema R = [{schema_str}]\n"
            f"Attribute set X = [{cand_str}]\n\n"

            "Schema description:\n"
            f"{schema_desc}\n"

            "TASK:\n"
            "Determine whether the attribute set X is a superkey of schema R.\n\n"

            "Superkey Definition:\n"
            "X is a superkey if X functionally determines all attributes in R.\n\n"

            "Important:\n"
            "- You MUST use reasoning based on the meaning of attributes and the real-world interpretation of the schema.\n"
            "- Any superset of a key is also a superkey.\n"
            "- A minimal key (candidate key) is also a superkey.\n\n"
            # "- Any team ID/name is unique even across different leagues. No teams from different leagues will play against each other!\n"
            # "- It is possible for a player to return to the same team multiple times within a single season, therefore having different stints.\n\n"
            # "- An award can have multiple recipients (i.e., multiple coaches may receive the same award in the same year and league).\n"
            # "- The same award name may exist across different leagues.\n"
            # "- The note field is non-unique and should be ignored when determining superkeys.\n\n"
            # "- In professional hockey, a team normally plays at most one game per day.\n\n"

            "Answer rules:\n"
            "- Output Yes if X is a superkey.\n"
            "- Output No otherwise.\n\n"

            "Return your answer in JSON format. No extra output. No overthinking.:\n"
            '{"answer": "Yes"} or {"answer": "No"}'
        )

    def build_prompt_4_all_min_keys(self, schema: List[str], schema_name: str) -> str:
        schema_str = ", ".join(schema)
        schema_desc = schema_desc_db(schema_name)

        return (
            "You are an expert in relational database schema design.\n\n"

            f"Relational schema R = [{schema_str}]\n\n"

            "Schema description:\n"
            f"{schema_desc}\n"

            "TASK:\n"
            "Please find all possible MINIMAL keys of the schema R.\n\n"

            "Key Definition:\n"
            "- A set of attributes X is a MINIMAL key (candidate key) if:\n"
            "  (1) X functionally determines all attributes in R, and\n"
            "  (2) No proper subset of X can functionally determine all attributes in R.\n\n"

            "Important:\n"
            "- You MUST use reasoning based on the meaning of attributes and the real-world interpretation of the schema.\n\n"

            "Output Format Instructions:\n"
            "- Use comma (,) to separate attributes within a key.\n"
            "- Use semicolon (;) to separate different minimal keys.\n"
            "- Return ONLY the JSON. No extra text.\n\n"

            "Output JSON format:\n"
            '{"answer": "attr1, attr2; attr3"}'
        )

    def build_prompt_4_prime_attributes(
            self,
            schema: List[str],
            schema_name: str,
    ) -> str:

        schema_str = ", ".join(schema)
        schema_desc = schema_desc_db(schema_name)

        return (
            "You are an expert in relational database schema design.\n\n"

            f"Relational schema R = [{schema_str}]\n\n"

            "Schema description:\n"
            f"{schema_desc}\n"

            "TASK:\n"
            "Identify all attributes that are LIKELY to be prime attributes from the schema.\n\n"

            "Prime Attribute Definition:\n"
            "- A prime attribute is an attribute that belongs to at least one candidate key.\n\n"

            "Important:\n"
            "- Use database semantics and real-world meaning of attributes.\n\n"

            "Output Rules:\n"
            "- Return ONLY attributes likely to be prime.\n"
            "- Use comma-separated attribute names.\n"
            "- Return JSON only.\n\n"

            'Output JSON format:\n'
            '{"answer": "attr1, attr2, attr3"}'
        )

    def _generate(self, prompt: str) -> str:
        messages = [{"role": "user", "content": prompt}]

        if hasattr(self.tokenizer, "apply_chat_template"):
            text = self.tokenizer.apply_chat_template(
                messages,
                tokenize=False,
                add_generation_prompt=True,
            )
        else:
            text = prompt

        model_inputs = self.tokenizer([text], return_tensors="pt")
        model_inputs = {k: v.to(self.model.device) for k, v in model_inputs.items()}

        with self._torch.no_grad():
            generated = self.model.generate(
                **model_inputs,
                max_new_tokens=self.max_new_tokens,
                do_sample=False,
                temperature=None,
                top_p=None,
                # repetition_penalty=1.1,
                # no_repeat_ngram_size=4,
                pad_token_id=getattr(self.tokenizer, "pad_token_id", None)
                             or getattr(self.tokenizer, "eos_token_id", None),
                eos_token_id=getattr(self.tokenizer, "eos_token_id", None),
            )

        new_tokens = generated[0][model_inputs["input_ids"].shape[1]:]
        text = self.tokenizer.decode(new_tokens, skip_special_tokens=True).strip()
        return text

    @staticmethod
    def parse_json_answer(text: str) -> Optional[bool]:
        """
        Returns:
            True  -> Yes
            False -> No
            None  -> cannot parse
        """
        if "</think>" in text:
            text = text.split("</think>")[-1]

        matches = re.findall(r"\{.*?\}", text, re.DOTALL)
        if not matches:
            return None

        last_json_str = matches[-1]

        try:
            obj = json.loads(last_json_str)
            val = obj.get("answer", "").strip().lower()
            if val == "yes":
                return True
            elif val == "no":
                return False
        except Exception:
            return None

        return None

    @staticmethod
    def parse_minimal_keys(text: str) -> Optional[List[Key]]:
        """
        Parse output like:
        {"answer": "a, b; c"}
        """

        if "</think>" in text:
            text = text.split("</think>")[-1]

        matches = re.findall(r"\{.*?\}", text, re.DOTALL)
        if not matches:
            return None

        last_json_str = matches[-1]

        try:
            obj = json.loads(last_json_str)
            answer = obj.get("answer", "").strip()

            if not answer:
                return None

            keys = []
            key_parts = [k.strip() for k in answer.split(";") if k.strip()]

            for part in key_parts:
                attrs = [a.strip() for a in part.split(",") if a.strip()]
                if attrs:
                    keys.append(Key(attrs))

            return keys

        except Exception:
            return None

    @staticmethod
    def parse_prime_attributes(text: str) -> Optional[List[str]]:
        if "</think>" in text:
            text = text.split("</think>")[-1]

        matches = re.findall(r"\{.*?\}", text, re.DOTALL)
        if not matches:
            return None

        last_json_str = matches[-1]

        try:
            obj = json.loads(last_json_str)
            answer = obj.get("answer", "").strip()

            if not answer:
                return []

            attrs = [a.strip() for a in answer.split(",") if a.strip()]
            return attrs

        except Exception:
            return None


class Interview:
    @staticmethod
    def isSuperkeyOfSomeKeys(keys: Collection[Key], oneKey: Key) -> bool:
        for key in keys:
            if oneKey.contains(key):
                return True
        return False

    @staticmethod
    def isTwoKeySetsEqual(keySet1: List[Key], keySet2: List[Key]) -> bool:
        minKeys1 = Utils.refineToMinimalKeys(list(keySet1))
        minKeys2 = Utils.refineToMinimalKeys(list(keySet2))
        return minKeys1.issuperset(minKeys2) and minKeys2.issuperset(minKeys1)

    @staticmethod
    def isSubsetOfAnySupersets(sets: Collection[Key], oneSet: Key) -> bool:
        for set_ in sets:
            if set(set_.getAttributes()).issuperset(oneSet.getAttributes()):
                return True
        return False

    @staticmethod
    def refineToMinimalKeys(keys1: Collection[Key]) -> Set[Key]:
        minimal_keys: Set[Key] = set()
        keys = list(keys1)
        keys.sort(key=lambda k: k.size())

        for key in keys:
            is_minimal = True
            for existing in minimal_keys:
                if key.contains(existing):
                    is_minimal = False
                    break
            if is_minimal:
                minimal_keys.add(key)

        return minimal_keys

    @staticmethod
    def refineToMaximalNonKeys(keys1: Collection[Key]) -> Set[Key]:
        maximal_non_keys: Set[Key] = set()
        keys = list(keys1)
        keys.sort(key=lambda k: k.size(), reverse=True)

        for key in keys:
            is_maximal = True
            for existing in maximal_non_keys:
                if existing.contains(key):
                    is_maximal = False
                    break
            if is_maximal:
                maximal_non_keys.add(key)

        return maximal_non_keys

    @staticmethod
    def computeCandKeySet4TopDown(
            traversalDirection: str,
            MinedKeySet: List[Key],
            CandKeySet: List[Key],
            NonKeySet: Set[Key],
            allNonKeySet: Set[Key],
            allMinedKeySet: Set[Key],
            preDeterminedKeys: List[Key],
    ) -> List[Key]:
        superKeys: Set[Key] = set(MinedKeySet)
        for key in MinedKeySet:
            for candKey in CandKeySet:
                if candKey.contains(key):
                    superKeys.add(candKey)
        CandKeySet = [ck for ck in CandKeySet if ck not in superKeys]

        subsetNonKeySet: Set[Key] = set(NonKeySet)
        for nonKey in NonKeySet:
            for candKey in CandKeySet:
                if nonKey.contains(candKey):
                    subsetNonKeySet.add(candKey)
        CandKeySet = [ck for ck in CandKeySet if ck not in subsetNonKeySet]

        MinedKeySet.clear()
        NonKeySet.clear()

        minKeys = Interview.refineToMinimalKeys(allMinedKeySet)
        allMinedKeySet.clear()
        allMinedKeySet.update(minKeys)

        maxNonKeys = Interview.refineToMaximalNonKeys(allNonKeySet)
        allNonKeySet.clear()
        allNonKeySet.update(maxNonKeys)

        for key in superKeys:
            for e in key.getAttributes():
                newCandKey = set(key.getAttributes())
                newCandKey.discard(e)
                CK = Key(newCandKey)
                if (
                        not Interview.isSubsetOfAnySupersets(allNonKeySet, CK)
                        and CK not in CandKeySet
                        and not Interview.isSuperkeyOfSomeKeys(allMinedKeySet, CK)
                        and not Interview.isSubsetOfAnySupersets(preDeterminedKeys, CK)
                ):
                    CandKeySet.append(CK)
                elif Interview.isSuperkeyOfSomeKeys(allMinedKeySet, CK):
                    MinedKeySet.append(CK)

        if traversalDirection == "dfs":
            CandKeySet.sort(key=lambda k: k.size())
        return CandKeySet

    @staticmethod
    def computeCandKeySet4BottomUp(
            R: List[str],
            traversalDirection: str,
            MinedKeySet: List[Key],
            CandKeySet: List[Key],
            NonKeySet: Set[Key],
            allNonKeySet: Set[Key],
            allMinedKeySet: Set[Key],
            preDeterminedKeys: List[Key],
    ) -> List[Key]:
        superKeys: Set[Key] = set(MinedKeySet)
        for key in MinedKeySet:
            for candKey in CandKeySet:
                if candKey.contains(key):
                    superKeys.add(candKey)
        CandKeySet = [ck for ck in CandKeySet if ck not in superKeys]

        subsetNonKeySet: Set[Key] = set(NonKeySet)
        for nonKey in NonKeySet:
            for candKey in CandKeySet:
                if nonKey.contains(candKey):
                    subsetNonKeySet.add(candKey)
        CandKeySet = [ck for ck in CandKeySet if ck not in subsetNonKeySet]

        MinedKeySet.clear()
        NonKeySet.clear()

        minKeys = Interview.refineToMinimalKeys(allMinedKeySet)
        allMinedKeySet.clear()
        allMinedKeySet.update(minKeys)

        maxNonKeys = Interview.refineToMaximalNonKeys(allNonKeySet)
        allNonKeySet.clear()
        allNonKeySet.update(maxNonKeys)

        for nonKey in subsetNonKeySet:
            diff = set(R)
            diff.difference_update(nonKey.getAttributes())
            for e in diff:
                newCandSet = set(nonKey.getAttributes())
                newCandSet.add(e)
                CK = Key(newCandSet)
                if (
                        not Interview.isSuperkeyOfSomeKeys(allMinedKeySet, CK)
                        and CK not in CandKeySet
                        and not Interview.isSubsetOfAnySupersets(allNonKeySet, CK)
                        and not Interview.isSubsetOfAnySupersets(preDeterminedKeys, CK)
                ):
                    CandKeySet.append(CK)
                elif (
                        Interview.isSubsetOfAnySupersets(allNonKeySet, CK)
                        or Interview.isSubsetOfAnySupersets(preDeterminedKeys, CK)
                ):
                    NonKeySet.add(CK)

        if traversalDirection == "dfs":
            CandKeySet.sort(key=lambda k: k.size(), reverse=True)
        return CandKeySet

    @staticmethod
    def renderSampleRelation(R: List[str], attrs: Collection[str]) -> str:
        col = ""
        for i, attr in enumerate(R):
            if i != len(R) - 1:
                col += f"{attr} | "
            else:
                col += attr

        line = "+" + ("-" * len(col)) + "+"
        firstRow = ""
        for i, attr in enumerate(R):
            space = " " * len(attr)
            if i != len(R) - 1:
                firstRow += f"0{space}| " if attr in attrs else f"*{space}| "
            else:
                firstRow += "0" if attr in attrs else "*"

        secRow = ""
        attrs_set = set(attrs)
        for i, attr in enumerate(R):
            space = " " * len(attr)
            value = "0" if attr in attrs_set else "*"
            if i != len(R) - 1:
                secRow += f"{value}{space}| "
            else:
                secRow += value

        return "\n".join([line, col, line, firstRow, line, secRow, line])

    @staticmethod
    def printSampleRelation(R: List[str], attrs: Collection[str]) -> None:
        print(Interview.renderSampleRelation(R, attrs))

    @staticmethod
    def isEmptyValue(keySet: Collection[Key]) -> bool:
        for key in keySet:
            if key.getAttributes():
                return False
        return True

    @staticmethod
    def isSuperKey(
            candidateKey: Key,
            minimalKeys: Optional[List[Key]] = None,
            oracle: Optional[KeyOracle] = None,
            schema: Optional[List[str]] = None,
            schema_name: Optional[str] = None,
            print_output: bool = False,
    ) -> bool:
        """
        Backward-compatible overload replacement.

        Use one of the following modes:
        1) exact mode with given minimal keys:
             isSuperKey(candidateKey, minimalKeys=givenMinimalKeys)
        2) LLM oracle mode:
             isSuperKey(candidateKey, oracle=qwen_oracle, schema=R)
        """
        if oracle is not None:
            if schema is None:
                raise ValueError("schema must be provided when oracle is used")
            return oracle.ask_is_key(schema, schema_name, candidateKey, print_output=print_output)

        if minimalKeys is None:
            raise ValueError("minimalKeys must be provided when oracle is not used")

        candidate_attrs = set(candidateKey.getAttributes())
        for minKey in minimalKeys:
            if candidate_attrs.issuperset(minKey.getAttributes()):
                return True
        return False

    @staticmethod
    def genInitialCandidateKeysWithInputKeys(
            R: List[str],
            traversalStart: str,
            inputKeys: List[Key],
            nonKeyInTheRound: Set[Key],
            minedKeysInTheRound: List[Key],
            allMineKeys: Set[Key],
            initialCandKeys: List[Key],
    ) -> Optional[List[Key]]:
        nonKeyInTheRound.clear()
        minedKeysInTheRound.clear()

        if traversalStart == "topdown":
            if inputKeys:
                candKeys: List[Key] = []
                allSets: List[Key] = []
                for set_ in initialCandKeys:
                    for a in set_.getAttributes():
                        candK = set(set_.getAttributes())
                        candK.discard(a)
                        CK = Key(candK)
                        if CK not in allSets:
                            allSets.append(CK)
                        if (
                                not Interview.isSuperkeyOfSomeKeys(allMineKeys, CK)
                                and CK not in candKeys
                                and not Interview.isSubsetOfAnySupersets(allMineKeys, CK)
                        ):
                            candKeys.append(CK)
                        if Interview.isSuperkeyOfSomeKeys(allMineKeys, CK):
                            minedKeysInTheRound.append(CK)
                if not candKeys and allSets and allSets[0].size() != 0:
                    return Interview.genInitialCandidateKeysWithInputKeys(
                        R,
                        traversalStart,
                        inputKeys,
                        nonKeyInTheRound,
                        minedKeysInTheRound,
                        allMineKeys,
                        allSets,
                    )
                return candKeys
            return initialCandKeys

        elif traversalStart == "bottomup":
            if inputKeys:
                candKeys: List[Key] = []
                allSets: List[Key] = []
                for set_ in initialCandKeys:
                    diff = list(R)
                    for a in set_.getAttributes():
                        if a in diff:
                            diff.remove(a)
                    for a in diff:
                        candK = set(set_.getAttributes())
                        candK.add(a)
                        CK = Key(candK)
                        if CK not in allSets:
                            allSets.append(CK)
                        if (
                                not Interview.isSubsetOfAnySupersets(allMineKeys, CK)
                                and CK not in candKeys
                                and not Interview.isSuperkeyOfSomeKeys(allMineKeys, CK)
                        ):
                            candKeys.append(CK)
                        if Interview.isSubsetOfAnySupersets(allMineKeys, CK):
                            nonKeyInTheRound.add(CK)
                if not candKeys and allSets and allSets[0].size() != len(R):
                    return Interview.genInitialCandidateKeysWithInputKeys(
                        R,
                        traversalStart,
                        inputKeys,
                        nonKeyInTheRound,
                        minedKeysInTheRound,
                        allMineKeys,
                        allSets,
                    )
                return candKeys
            return initialCandKeys

        else:
            raise ValueError("Not supported interview strategy! (topdown/bottomup only)")

    @staticmethod
    def interview_with_probability(
            traversalStart: str,
            traversalDirection: str,
            R: List[str],
            p: float,
            print_output: bool,
            rng: Optional[Random] = None,
    ) -> List[object]:
        if print_output:
            print(f"Given schema: {R}")

        candKeySet: List[Key] = []
        if traversalStart == "topdown":
            candKeySet.append(Key(R))
        elif traversalStart == "bottomup":
            candKeySet.append(Key([]))
        else:
            raise ValueError("Not supported interview strategy! (topdown/bottomup only)")

        num4NOAnswers = 0
        num4AllAnswers = 0
        allMinedKeys: Set[Key] = set()
        allNonKeys: Set[Key] = set()
        round_num = 0
        rng = rng or Random()

        while True:
            round_num += 1

            if print_output:
                print(f"\n***************Round {round_num}***************")
                print(f"current candidate key sets num: {len(candKeySet)}")
                print("current candidate key sets:")
                for candKey in candKeySet:
                    print(f"candidate key: {candKey}")
                print("-----------------------------\n")

            NonKeys: Set[Key] = set()
            minedKeysInTheRound: List[Key] = []

            for candKey in candKeySet:
                if print_output:
                    print(f'Interviewing "{candKey}" if it is a minimal key...\nGiven the sample:')
                    Interview.printSampleRelation(R, candKey.getAttributes())
                    print(
                        f"Is it possible whether there are two records that have values in {candKey} that are matching?"
                    )

                num4AllAnswers += 1
                if rng.random() < p:
                    allMinedKeys.add(candKey)
                    minedKeysInTheRound.append(candKey)
                    num4NOAnswers += 1

                    if print_output:
                        print("Answer: No")
                        print("current Key in the round:")
                        Utils.printKeys(minedKeysInTheRound)

                    if traversalStart == "topdown" and traversalDirection == "dfs":
                        break
                else:
                    NonKeys.add(candKey)
                    allNonKeys.add(candKey)

                    if print_output:
                        print("Answer: Yes\n")

                    if traversalStart == "bottomup" and traversalDirection == "dfs":
                        break

            if print_output:
                print("The candidate key sets for current round below: ")
                for candKey in candKeySet:
                    print(candKey)
                print("newly mined Keys in this round below: ")
                Utils.printKeys(minedKeysInTheRound)

            if traversalStart == "topdown":
                candKeySet = Interview.computeCandKeySet4TopDown(
                    traversalDirection,
                    minedKeysInTheRound,
                    candKeySet,
                    NonKeys,
                    allNonKeys,
                    allMinedKeys,
                    [],
                )
            else:
                candKeySet = Interview.computeCandKeySet4BottomUp(
                    R,
                    traversalDirection,
                    minedKeysInTheRound,
                    candKeySet,
                    NonKeys,
                    allNonKeys,
                    allMinedKeys,
                    [],
                )

            if Interview.isEmptyValue(candKeySet):
                if print_output:
                    print("\n\nInterview is finished because candidate key set for next round is empty!\n\n")
                break
            else:
                if print_output:
                    print(
                        "Based on candidate key set of current round, next round's candidate key set for only newly mined keys: ")
                    for candKey in candKeySet:
                        print(f"candidate key: {candKey}")
                    print("+++++++++++++++++++++++++++++\n")
        allMinedKeys = Interview.refineToMinimalKeys(allMinedKeys)  # refine interviewed keys to minimal
        return [allMinedKeys, num4NOAnswers, num4AllAnswers, round_num]

    @staticmethod
    def interview_with_given_minimal_keys(
            traversalStart: str,
            traversalDirection: str,
            R: List[str],
            print_output: bool,
            givenMinimalKeys: List[Key],
    ) -> List[object]:
        if print_output:
            print(f"Given schema: {R}")

        candKeySet: List[Key] = []
        if traversalStart == "topdown":
            candKeySet.append(Key(R))
        elif traversalStart == "bottomup":
            candKeySet.append(Key([]))
        else:
            raise ValueError("Not supported interview strategy! (topdown/bottomup only)")

        num4NOAnswers = 0
        num4AllAnswers = 0
        allMinedKeys: Set[Key] = set()
        allNonKeys: Set[Key] = set()
        round_num = 0

        while True:
            round_num += 1

            if print_output:
                print(f"\n***************Round {round_num}***************")
                print(f"current candidate key sets num: {len(candKeySet)}")
                print("current candidate key sets:")
                for candKey in candKeySet:
                    print(f"candidate key: {candKey}")
                print("-----------------------------\n")

            NonKeys: Set[Key] = set()
            minedKeysInTheRound: List[Key] = []

            for candKey in candKeySet:
                if print_output:
                    print(f'Interviewing "{candKey}" if it is a minimal key...\nGiven the sample:')
                    Interview.printSampleRelation(R, candKey.getAttributes())
                    print(
                        f"Is it possible whether there are two records that have values in {candKey} that are matching?"
                    )

                num4AllAnswers += 1
                if Interview.isSuperKey(candKey, minimalKeys=givenMinimalKeys):
                    allMinedKeys.add(candKey)
                    minedKeysInTheRound.append(candKey)
                    num4NOAnswers += 1

                    if print_output:
                        print("Answer: No")
                        print("current Key in the round:")
                        Utils.printKeys(minedKeysInTheRound)

                    if traversalStart == "topdown" and traversalDirection == "dfs":
                        break
                else:
                    NonKeys.add(candKey)
                    allNonKeys.add(candKey)

                    if print_output:
                        print("Answer: Yes\n")

                    if traversalStart == "bottomup" and traversalDirection == "dfs":
                        break

            if print_output:
                print("The candidate key sets for current round below: ")
                for candKey in candKeySet:
                    print(candKey)
                print("newly mined Keys in this round below: ")
                Utils.printKeys(minedKeysInTheRound)

            if traversalStart == "topdown":
                candKeySet = Interview.computeCandKeySet4TopDown(
                    traversalDirection,
                    minedKeysInTheRound,
                    candKeySet,
                    NonKeys,
                    allNonKeys,
                    allMinedKeys,
                    [],
                )
            else:
                candKeySet = Interview.computeCandKeySet4BottomUp(
                    R,
                    traversalDirection,
                    minedKeysInTheRound,
                    candKeySet,
                    NonKeys,
                    allNonKeys,
                    allMinedKeys,
                    [],
                )

            if Interview.isEmptyValue(candKeySet):
                if print_output:
                    print("\n\nInterview is finished because candidate key set for next round is empty!\n\n")
                break
            else:
                if print_output:
                    print(
                        "Based on candidate key set of current round, next round's candidate key set for only newly mined keys: ")
                    for candKey in candKeySet:
                        print(f"candidate key: {candKey}")
                    print("+++++++++++++++++++++++++++++\n")
        allMinedKeys = Interview.refineToMinimalKeys(allMinedKeys)  # refine interviewed keys to minimal
        return [allMinedKeys, num4NOAnswers, num4AllAnswers, round_num]

    @staticmethod
    def interview_with_predetermined_keys(
            traversalStart: str,
            traversalDirection: str,
            R: List[str],
            print_output: bool,
            givenMinimalKeys: List[Key],
            preDeterminedKeys: List[Key],
    ) -> List[object]:
        if print_output:
            print(f"Given schema: {R}")

        candKeySet: List[Key] = []
        num4NOAnswers = 0
        num4AllAnswers = 0
        allMinedKeys: Set[Key] = set(preDeterminedKeys)
        allNonKeys: Set[Key] = set()
        NonKeysInTheRound: Set[Key] = set()
        minedKeysInTheRound: List[Key] = []
        round_num = 0

        if traversalStart == "topdown":
            candKeySet.append(Key(R))
        elif traversalStart == "bottomup":
            candKeySet.append(Key([]))
        else:
            raise ValueError("Not supported interview strategy! (topdown/bottomup only)")

        candKeySet = Interview.genInitialCandidateKeysWithInputKeys(
            R,
            traversalStart,
            preDeterminedKeys,
            NonKeysInTheRound,
            minedKeysInTheRound,
            allMinedKeys,
            candKeySet,
        ) or []

        while True:
            round_num += 1
            if print_output:
                print(f"\n***************Round {round_num}***************")
                print(f"current candidate key sets num: {len(candKeySet)}")
                print("current candidate key sets:")
                for candKey in candKeySet:
                    print(f"candidate key: {candKey}")
                print("-----------------------------\n")

            for candKey in candKeySet:
                if print_output:
                    print("\n" + "#" * 40)
                    print(f'Interviewing "{candKey}" if it is a key...\nGiven the sample:')
                    Interview.printSampleRelation(R, candKey.getAttributes())
                    print(
                        f"Is it possible whether there are two records that have values in {candKey} that are matching?"
                    )

                num4AllAnswers += 1
                if Interview.isSuperKey(candKey, minimalKeys=givenMinimalKeys):
                    allMinedKeys.add(candKey)
                    minedKeysInTheRound.append(candKey)
                    num4NOAnswers += 1

                    if print_output:
                        print("Answer: No")
                        print("current Key in the round:")
                        Utils.printKeys(minedKeysInTheRound)

                    if traversalStart == "topdown" and traversalDirection == "dfs":
                        break
                else:
                    NonKeysInTheRound.add(candKey)
                    allNonKeys.add(candKey)

                    if print_output:
                        print("Answer: Yes\n")

                    if traversalStart == "bottomup" and traversalDirection == "dfs":
                        break

            if print_output:
                print("The candidate key sets for current round below: ")
                for candKey in candKeySet:
                    print(candKey)
                print("newly mined Keys in this round below: ")
                Utils.printKeys(minedKeysInTheRound)

            if traversalStart == "topdown":
                candKeySet = Interview.computeCandKeySet4TopDown(
                    traversalDirection,
                    minedKeysInTheRound,
                    candKeySet,
                    NonKeysInTheRound,
                    allNonKeys,
                    allMinedKeys,
                    preDeterminedKeys,
                )
            else:
                candKeySet = Interview.computeCandKeySet4BottomUp(
                    R,
                    traversalDirection,
                    minedKeysInTheRound,
                    candKeySet,
                    NonKeysInTheRound,
                    allNonKeys,
                    allMinedKeys,
                    preDeterminedKeys,
                )

            if (
                    Interview.isEmptyValue(candKeySet)
                    and not NonKeysInTheRound
                    and not minedKeysInTheRound
            ):
                if print_output:
                    print("\n\nInterview is finished because candidate key set for next round is empty!\n\n")
                break
            else:
                if print_output:
                    print(
                        "Based on candidate key set of current round, next round's candidate key set for only newly mined keys: ")
                    for candKey in candKeySet:
                        print(f"candidate key: {candKey}")
                    print("+++++++++++++++++++++++++++++\n")
        allMinedKeys = Interview.refineToMinimalKeys(allMinedKeys)  # refine interviewed keys to minimal
        return [allMinedKeys, num4NOAnswers, num4AllAnswers, round_num]

    @staticmethod
    def interview_with_llm_oracle(
            traversalStart: str,
            traversalDirection: str,
            R: List[str],
            schema_name: str,
            print_output: bool,
            oracle: KeyOracle,
            preDeterminedKeys: Optional[List[Key]] = None,
    ) -> List[object]:
        """
        New mode:
        - no probability p
        - no ground-truth minimal keys required
        - use LLM oracle to answer each interview question

        `preDeterminedKeys` is optional. If you want a fully LLM-driven experiment,
        pass None or [].
        """
        if print_output:
            print(f"Given schema: {R}")

        preDeterminedKeys = preDeterminedKeys or []
        candKeySet: List[Key] = []
        num4NOAnswers = 0
        num4AllAnswers = 0
        allMinedKeys: Set[Key] = set(preDeterminedKeys)
        allNonKeys: Set[Key] = set()
        NonKeysInTheRound: Set[Key] = set()
        minedKeysInTheRound: List[Key] = []
        round_num = 0

        if traversalStart == "topdown":
            candKeySet.append(Key(R))
        elif traversalStart == "bottomup":
            candKeySet.append(Key([]))
        else:
            raise ValueError("Not supported interview strategy! (topdown/bottomup only)")

        candKeySet = Interview.genInitialCandidateKeysWithInputKeys(
            R,
            traversalStart,
            preDeterminedKeys,
            NonKeysInTheRound,
            minedKeysInTheRound,
            allMinedKeys,
            candKeySet,
        ) or candKeySet

        while True:
            round_num += 1
            if print_output:
                print(f"\n***************Round {round_num}***************")
                print(f"current candidate key sets num: {len(candKeySet)}")
                print("current candidate key sets:")
                for candKey in candKeySet:
                    print(f"candidate key: {candKey}")
                print("-----------------------------\n")

            for candKey in candKeySet:
                num4AllAnswers += 1
                if print_output:
                    print(f"\n***************Question {num4AllAnswers}***************")
                    print(f'Interviewing "{candKey}" if it is a key with LLMs...')
                    # print(f'Interviewing "{candKey}" if it is a key...\nGiven the sample:')
                    # Interview.printSampleRelation(R, candKey.getAttributes())
                    # print(
                    #     f"Is it possible whether there are two records that have values in {candKey} that are matching?"
                    # )

                if Interview.isSuperKey(
                        candKey,
                        oracle=oracle,
                        schema=R,
                        schema_name=schema_name,
                        print_output=print_output,
                ):
                    allMinedKeys.add(candKey)
                    minedKeysInTheRound.append(candKey)
                    num4NOAnswers += 1

                    if print_output:
                        print("Answer: It is a key")
                        print("current Key in the round:")
                        Utils.printKeys(minedKeysInTheRound)

                    if traversalStart == "topdown" and traversalDirection == "dfs":
                        break
                else:
                    NonKeysInTheRound.add(candKey)
                    allNonKeys.add(candKey)

                    if print_output:
                        print("Answer: It is NOT a key\n")

                    if traversalStart == "bottomup" and traversalDirection == "dfs":
                        break

            if print_output:
                print("The candidate key sets for current round below: ")
                for candKey in candKeySet:
                    print(candKey)
                print("newly mined Keys in this round below: ")
                Utils.printKeys(minedKeysInTheRound)

            if traversalStart == "topdown":
                candKeySet = Interview.computeCandKeySet4TopDown(
                    traversalDirection,
                    minedKeysInTheRound,
                    candKeySet,
                    NonKeysInTheRound,
                    allNonKeys,
                    allMinedKeys,
                    preDeterminedKeys,
                )
            else:
                candKeySet = Interview.computeCandKeySet4BottomUp(
                    R,
                    traversalDirection,
                    minedKeysInTheRound,
                    candKeySet,
                    NonKeysInTheRound,
                    allNonKeys,
                    allMinedKeys,
                    preDeterminedKeys,
                )

            if Interview.isEmptyValue(candKeySet):
                if print_output:
                    print("\n\nInterview is finished because candidate key set for next round is empty!\n\n")
                break
            else:
                if print_output:
                    print(
                        "Based on candidate key set of current round, next round's candidate key set for only newly mined keys: ")
                    for candKey in candKeySet:
                        print(f"candidate key: {candKey}")
                    print("+++++++++++++++++++++++++++++\n")

            NonKeysInTheRound = set()
            minedKeysInTheRound = []

        allMinedKeys = Interview.refineToMinimalKeys(allMinedKeys)  # refine interviewed keys to minimal
        return [allMinedKeys, num4AllAnswers]


__all__ = [
    "Key",
    "Utils",
    "KeyOracle",
    "KeyInterviewOracle",
    "Interview",
]


def evaluate_keys(groundTruthMinimalKeys, interviewedMinimalKeys):
    gt_set = set(groundTruthMinimalKeys)
    pred_set = set(interviewedMinimalKeys)

    # True Positives
    tp = len(gt_set & pred_set)

    # Precision
    precision = tp / len(pred_set) if pred_set else 0.0

    # Recall
    recall = tp / len(gt_set) if gt_set else 0.0

    # F1
    if precision + recall == 0:
        f1 = 0.0
    else:
        f1 = 2 * precision * recall / (precision + recall)

    return precision, recall, f1


def evaluate_keys_relaxed(groundTruthMinimalKeys, interviewedKeys):
    gt_set = set(groundTruthMinimalKeys)
    pred_set = set(interviewedKeys)

    tp_recall = 0
    for k in gt_set:
        fine = False
        for k1 in pred_set:
            if k.contains(k1):  # k is superkey
                fine = True
                break
        if fine:
            tp_recall += 1
    recall = tp_recall / len(gt_set) if gt_set else 0.0

    tp_precision = 0
    for k in pred_set:
        fine = False
        for k1 in gt_set:
            if k.contains(k1):  # k is superkey
                fine = True
                break
        if fine:
            tp_precision += 1
    precision = tp_precision / len(pred_set) if pred_set else 0.0

    # F1
    if precision + recall == 0:
        f1 = 0.0
    else:
        f1 = 2 * precision * recall / (precision + recall)

    return precision, recall, f1


def evaluate_prime_attributes(
        ground_truth_prime_attrs,
        predicted_prime_attrs,
):
    gt_set = set(ground_truth_prime_attrs)
    pred_set = set(predicted_prime_attrs)

    tp = len(gt_set & pred_set)

    precision = tp / len(pred_set) if pred_set else 0.0
    recall = tp / len(gt_set) if gt_set else 0.0

    if precision + recall == 0:
        f1 = 0.0
    else:
        f1 = 2 * precision * recall / (precision + recall)

    return precision, recall, f1


def compute_ground_truth_prime_attributes(
        groundTruthMinimalKeys
):
    prime_attrs = set()

    for key in groundTruthMinimalKeys:
        prime_attrs.update(key.getAttributes())

    return sorted(prime_attrs)


def compute_topk_freq_minimal_keys(keys_frequency_dict):
    freq_groups = defaultdict(set)
    for k, v in keys_frequency_dict.items():
        freq_groups[v].add(k)

    sorted_freqs = sorted(freq_groups.keys(), reverse=True)

    current_keys = set()
    results = []
    seen_models = set()

    for freq in sorted_freqs:
        current_keys |= freq_groups[freq]
        minimal_keys = Interview.refineToMinimalKeys(current_keys)

        # 转成 frozenset 用于去重
        model_signature = frozenset(minimal_keys)

        if model_signature not in seen_models:
            seen_models.add(model_signature)
            results.append((freq, minimal_keys))

    return results


if __name__ == "__main__":
    model_name_or_path = "deepseek-ai/DeepSeek-R1-Distill-Llama-70B"
    # model_name_or_path = "Qwen/Qwen3-30B-A3B-Thinking-2507"
    oracle = KeyInterviewOracle(
        model_name_or_path=model_name_or_path,
        device_map="auto",
        max_new_tokens=10000
    )
    R_reduction = True
    for schema_name in ['team_splits']:
        R, groundTruthMinimalKeys = schema_info_db(schema_name)
        print(f"Schema Name: {schema_name} | Schema: {R} | Ground Truth Minimal Keys: {groundTruthMinimalKeys}")

        """
        [Instruct]
        Qwen/Qwen3-4B-Instruct-2507, Qwen/Qwen3-30B-A3B-Instruct-2507
        meta-llama/Llama-3.2-3B-Instruct, meta-llama/Llama-3.3-70B-Instruct
        Qwen/Qwen3-Next-80B-A3B-Instruct
        """
        """
        [Thinking]
        Qwen/Qwen3-30B-A3B-Thinking-2507, Qwen/Qwen3-4B-Thinking-2507, Qwen/Qwen3-Next-80B-A3B-Thinking(CUDA OOM)
        deepseek-ai/DeepSeek-R1-Distill-Llama-70B, deepseek-ai/DeepSeek-R1-Distill-Qwen-7B, deepseek-ai/DeepSeek-R1-Distill-Llama-8B 
        deepseek-ai/DeepSeek-R1-Distill-Qwen-32B, deepseek-ai/DeepSeek-R1-Distill-Qwen-14B
        """

        print(f"Using LLM Model: {model_name_or_path} for interview...")
        model_short_name = model_name_or_path.split("/")[-1]
        res_path = f"./result_R_reduction={R_reduction}_{model_short_name}_{schema_name}.txt"
        all_mined_min_keys_dict = {}
        total_cost_min = 0

        with open(res_path, "a", encoding="utf-8") as f:
            f.write("#" * 20 + "\n")
            f.write(
                f"Schema Name: {schema_name} | Schema: {R} | Ground Truth Minimal Keys: {groundTruthMinimalKeys}\nUsing LLM Model: {model_name_or_path} for interview...\n")

        direct_res = "\n===== Direct Evaluation =====\n"
        direct_keys = oracle.ask_all_minimal_keys(
            schema=R,
            schema_name=schema_name,
            print_output=True
        )
        direct_res += f"Direct LLM minimal keys: {direct_keys}\n"
        precision, recall, f1 = evaluate_keys(
            groundTruthMinimalKeys,
            direct_keys
        )
        precision_rel, recall_rel, f1_rel = evaluate_keys_relaxed(
            groundTruthMinimalKeys,
            direct_keys
        )
        direct_res += f"F1, recall, precision: {f1:.4f}, {recall:.4f}, {precision:.4f}\n"
        direct_res += f"Relaxed F1, recall, precision: {f1_rel:.4f}, {recall_rel:.4f}, {precision_rel:.4f}\n"
        direct_res += "=============================\n\n"
        with open(res_path, "a", encoding="utf-8") as f:
            f.write(direct_res + "\n")

        '''LLM for reducing schema'''
        if R_reduction:
            prime_res = ""
            ground_truth_prime_attrs = compute_ground_truth_prime_attributes(
                groundTruthMinimalKeys
            )
            predicted_prime_attrs = oracle.ask_prime_attributes(
                schema=R,
                schema_name=schema_name,
                print_output=True
            )
            precision_prime, recall_prime, f1_prime = evaluate_prime_attributes(
                ground_truth_prime_attrs,
                predicted_prime_attrs
            )
            prime_res += f"Ground truth prime attrs: {ground_truth_prime_attrs}\n"
            prime_res += f"Predicted prime attrs: {predicted_prime_attrs}\n"
            prime_res += f"Prime Attr F1, recall, precision: {f1_prime:.4f}, {recall_prime:.4f}, {precision_prime:.4f}\n"
            R_prime = [a for a in R if a in predicted_prime_attrs]
            prime_res += f"Reduced schema after prime pruning: {R_prime}\n"
            prime_res += "=============================\n"
            R = R_prime
            with open(res_path, "a", encoding="utf-8") as f:
                f.write(prime_res + "\n")

        for traversalStart in ["topdown", "bottomup"]:
            for traversalDirection in ["dfs", "bfs"]:
                start_time = time.perf_counter()
                result = Interview.interview_with_llm_oracle(
                    traversalStart=traversalStart,  # 或 "bottomup"
                    traversalDirection=traversalDirection,  # 或 "dfs"
                    R=R,
                    schema_name=schema_name,
                    print_output=True,
                    oracle=oracle,
                    preDeterminedKeys=[],  # 不用 predetermined keys 就传 []
                )
                end_time = time.perf_counter()
                elapsed_min = (end_time - start_time) / 60.0
                total_cost_min += elapsed_min

                interviewedMinimalKeys = result[0]

                for k in interviewedMinimalKeys:
                    if k not in all_mined_min_keys_dict:
                        all_mined_min_keys_dict[k] = 0
                    all_mined_min_keys_dict[k] += 1

                num_all_questions = result[1]

                precision, recall, f1 = evaluate_keys(
                    groundTruthMinimalKeys,
                    interviewedMinimalKeys
                )

                precision_rel, recall_rel, f1_rel = evaluate_keys_relaxed(
                    groundTruthMinimalKeys,
                    interviewedMinimalKeys
                )

                result = ""
                result += "\n======= Evaluation =======\n"
                result += f"{traversalStart} {traversalDirection}\n"
                result += f"Time cost: {elapsed_min:.4f} minutes\n"
                result += f"Mined minimal keys: {interviewedMinimalKeys} | # Questions: {num_all_questions}\n"
                result += f"F1, recall, precision: {f1:.4f}, {recall:.4f}, {precision:.4f}\n"
                result += f"Relaxed F1, recall, precision: {f1_rel:.4f}, {recall_rel:.4f}, {precision_rel:.4f}\n"
                result += "==========================\n\n"
                print(result)
                with open(res_path, "a", encoding="utf-8") as f:
                    f.write(result)

        # metrics on aggregation
        keys_set = set(all_mined_min_keys_dict.keys())
        precision, recall, f1 = evaluate_keys(
            groundTruthMinimalKeys,
            keys_set
        )

        precision_rel, recall_rel, f1_rel = evaluate_keys_relaxed(
            groundTruthMinimalKeys,
            keys_set
        )

        # minimized keys in order of frequency
        minimized_key_sets_with_freq = compute_topk_freq_minimal_keys(all_mined_min_keys_dict)
        res_str = ""
        for freq, min_keys in minimized_key_sets_with_freq:
            precision_min, recall_min, f1_min = evaluate_keys(
                groundTruthMinimalKeys,
                min_keys
            )
            precision_min_rel, recall_min_rel, f1_min_rel = evaluate_keys_relaxed(
                groundTruthMinimalKeys,
                min_keys
            )
            res_str += f"Minimization of keys of freq >= {freq}, F1, recall, precision: {f1_min:.4f}, {recall_min:.4f}, {precision_min:.4f}\n"
            res_str += f"Minimization of keys of freq >= {freq}, Relaxed F1, recall, precision: {f1_min_rel:.4f}, {recall_min_rel:.4f}, {precision_min_rel:.4f}\n"

        result = ""
        result += "\n===== Evaluation after Aggregation =====\n"
        result += f"Total time cost: {total_cost_min:.4f} minutes\n"
        result += f"Mined minimal keys with frequency: {all_mined_min_keys_dict}\n"
        result += "\n===== Metrics with Non-Minimal Keys after Aggregation =====\n"
        result += f"F1, recall, precision: {f1:.4f}, {recall:.4f}, {precision:.4f}\n"
        result += f"Relaxed F1, recall, precision: {f1_rel:.4f}, {recall_rel:.4f}, {precision_rel:.4f}\n"
        result += "\n===== Metrics with Minimal Keys of Top-k Frequency after Aggregation =====\n"
        result += res_str
        result += "========================================\n\n"
        print(result)
        with open(res_path, "a", encoding="utf-8") as f:
            f.write(result)
            f.write("#" * 20 + "\n\n")
