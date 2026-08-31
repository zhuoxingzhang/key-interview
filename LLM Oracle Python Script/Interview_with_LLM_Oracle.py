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

    # ============== Dualize and Advance (DA) strategy family ==============
    # Adapted from the Dualize and Advance algorithm (All_MSS/AMAK) of
    # Gunopulos, Khardon, Mannila, Saluja, Toivonen, Sharma, TODS 28(2), 2003,
    # instantiated over the same two dimensions as the level-based suite:
    # traversalStart in {bottomup, topdown} and traversalDirection in {dfs, bfs}.
    # bottomup: maintain maximal anti-keys; candidates are the minimal
    #   transversals of their complements (candidate minimal keys); an anti-key
    #   answer is a counterexample that is advanced upward to a maximal anti-key.
    # topdown (dual): maintain minimal keys; candidates are the complements of
    #   the minimal transversals of the key hypergraph (candidate maximal
    #   anti-keys); a key answer is a counterexample that is shrunk downward to
    #   a minimal key.
    # dfs: handle the first counterexample immediately; bfs: interview the whole
    #   candidate batch, then handle all counterexamples of the batch.

    @staticmethod
    def isProperSubsetOfSomeKeys(minimalKeys: Collection[Key], oneSet: Key) -> bool:
        """a proper subset of a given minimal key is an anti-key by minimality"""
        for key in minimalKeys:
            if key.contains(oneSet) and key.size() > oneSet.size():
                return True
        return False

    @staticmethod
    def _isMinimalHittingSet(S: Set[str], hyperEdges: List[Set[str]]) -> bool:
        for a in S:
            reduced = S - {a}
            if all(reduced & edge for edge in hyperEdges):
                return False
        return True

    @staticmethod
    def _mmcs(candidates: Set[str], hyperEdges: List[Set[str]], current: Set[str],
              output: Set[frozenset]) -> None:
        all_hit = True
        for edge in hyperEdges:
            if not (current & edge):
                all_hit = False
                break

        if all_hit:
            if Interview._isMinimalHittingSet(current, hyperEdges):
                output.add(frozenset(current))
            return

        edgeToCover = None
        for edge in hyperEdges:
            if not (current & edge):
                if edgeToCover is None or len(edge) < len(edgeToCover):
                    edgeToCover = edge
        if edgeToCover is None:
            return

        for attr in list(edgeToCover):
            if attr not in candidates:
                continue
            Interview._mmcs(candidates - {attr}, hyperEdges, current | {attr}, output)

    @staticmethod
    def computeCandKeySet4Dualize(R: List[str], maxAntiKeys: Collection[Key]) -> List[Key]:
        """
        Dualization step (bottom-up flavor): the candidate minimal keys are the
        minimal transversals of the hypergraph whose edges are the complements
        of the maximal anti-keys found so far.
        """
        hyperEdges: List[Set[str]] = []
        for antiKey in maxAntiKeys:
            hyperEdges.append(set(R) - set(antiKey.getAttributes()))
        minimalTransversals: Set[frozenset] = set()
        Interview._mmcs(set(R), hyperEdges, set(), minimalTransversals)
        candKeys = [Key(tr) for tr in minimalTransversals]
        candKeys.sort(key=lambda k: (k.size(), ",".join(sorted(k.getAttributes()))))  # increasing, deterministic
        return candKeys

    @staticmethod
    def computeCandKeySet4DualizeTopDown(R: List[str], minKeys: Collection[Key]) -> List[Key]:
        """
        Dual dualization step (top-down flavor): the candidate maximal anti-keys
        are the complements of the minimal transversals of the hypergraph whose
        edges are the minimal keys found so far.
        """
        hyperEdges: List[Set[str]] = [set(k.getAttributes()) for k in minKeys]
        minimalTransversals: Set[frozenset] = set()
        Interview._mmcs(set(R), hyperEdges, set(), minimalTransversals)
        candAntiKeys = [Key(set(R) - set(tr)) for tr in minimalTransversals]
        candAntiKeys.sort(key=lambda k: (-k.size(), ",".join(sorted(k.getAttributes()))))  # decreasing, deterministic
        return candAntiKeys

    @staticmethod
    def _interview_dualize_core(
            R: List[str],
            answer_is_key,
            print_output: bool,
            preDeterminedKeys: List[Key],
            traversalStart: str = "bottomup",
            traversalDirection: str = "dfs",
    ) -> List[object]:
        if print_output:
            print(f"Given schema: {R}")

        topdown = traversalStart == "topdown"
        if not topdown and traversalStart != "bottomup":
            raise ValueError("Not supported dualize strategy! (topdown/bottomup only)")
        bfs = traversalDirection == "bfs"

        counters = [0, 0]  # [0]: "No" answers (keys), [1]: all questions
        allMinedKeys: Set[Key] = set(preDeterminedKeys)  # known keys
        allNonKeys: Set[Key] = set()  # known anti-keys
        maxAntiKeys: Set[Key] = set()  # maximal anti-keys discovered so far
        minKeysFound: Set[Key] = set(preDeterminedKeys)  # minimal keys discovered so far
        round_num = 0

        def ask(cand: Key, label: str) -> bool:
            counters[1] += 1
            if print_output:
                print(f"\n***************Question {counters[1]}***************")
                print(f'Interviewing "{cand}" if it is {label}...')
            is_key = answer_is_key(cand)
            if is_key:
                counters[0] += 1
                if print_output:
                    print("Answer: No (it is a key)\n")
            elif print_output:
                print("Answer: Yes (it is an anti-key)\n")
            return is_key

        def advance(counterExample: Key) -> Key:
            """advance an anti-key greedily to a maximal anti-key"""
            antiKey = set(counterExample.getAttributes())
            if print_output:
                print(f"Advancing anti-key {counterExample} to a maximal anti-key...")
            for e in R:
                if e in antiKey:
                    continue
                EK = Key(antiKey | {e})
                if Interview.isSuperkeyOfSomeKeys(allMinedKeys, EK):  # implied key
                    continue
                if Interview.isSubsetOfAnySupersets(allNonKeys, EK) \
                        or Interview.isProperSubsetOfSomeKeys(preDeterminedKeys, EK):  # implied anti-key
                    antiKey.add(e)
                    allNonKeys.add(EK)
                    continue
                if ask(EK, "an anti-key"):
                    allMinedKeys.add(EK)
                else:
                    antiKey.add(e)
                    allNonKeys.add(EK)
            return Key(antiKey)

        def shrink(counterExample: Key) -> Key:
            """shrink a key greedily to a minimal key"""
            key = set(counterExample.getAttributes())
            if print_output:
                print(f"Shrinking key {counterExample} to a minimal key...")
            for e in R:
                if e not in key:
                    continue
                SK = Key(key - {e})
                if Interview.isSubsetOfAnySupersets(allNonKeys, SK) \
                        or Interview.isProperSubsetOfSomeKeys(preDeterminedKeys, SK):  # implied anti-key
                    continue
                if Interview.isSuperkeyOfSomeKeys(allMinedKeys, SK):  # implied key
                    key.discard(e)
                    continue
                if ask(SK, "a key"):
                    allMinedKeys.add(SK)
                    key.discard(e)
                else:
                    allNonKeys.add(SK)
            return Key(key)

        while True:
            round_num += 1
            candKeySet = (Interview.computeCandKeySet4DualizeTopDown(R, minKeysFound) if topdown
                          else Interview.computeCandKeySet4Dualize(R, maxAntiKeys))  # dualize

            if print_output:
                print(f"\n***************Round {round_num}***************")
                print(f"current candidate sets num: {len(candKeySet)}")
                print("current candidate maximal anti-keys:" if topdown
                      else "current candidate minimal keys:")
                for candKey in candKeySet:
                    print(f"candidate: {candKey}")
                print("-----------------------------\n")

            counterExamples: List[Key] = []
            for candKey in candKeySet:
                if topdown:
                    if Interview.isSubsetOfAnySupersets(allNonKeys, candKey):  # confirmed anti-key
                        continue
                    if Interview.isProperSubsetOfSomeKeys(preDeterminedKeys, candKey):  # implied anti-key
                        maxAntiKeys.add(candKey)
                        allNonKeys.add(candKey)
                        continue
                    if ask(candKey, "a maximal anti-key"):  # counterexample: an uncovered key
                        allMinedKeys.add(candKey)
                        counterExamples.append(candKey)
                        if not bfs:
                            break
                    else:  # maximal anti-key by dualization
                        allNonKeys.add(candKey)
                        maxAntiKeys.add(candKey)
                else:
                    if Interview.isSuperkeyOfSomeKeys(allMinedKeys, candKey):  # confirmed key
                        continue
                    if Interview.isProperSubsetOfSomeKeys(preDeterminedKeys, candKey):  # implied anti-key
                        allNonKeys.add(candKey)
                        counterExamples.append(candKey)
                        if not bfs:
                            break
                        continue
                    if ask(candKey, "a minimal key"):  # minimal key by dualization
                        allMinedKeys.add(candKey)
                        minKeysFound.add(candKey)
                    else:  # counterexample: an uncovered anti-key
                        allNonKeys.add(candKey)
                        counterExamples.append(candKey)
                        if not bfs:
                            break

            if not counterExamples:  # every candidate is confirmed
                if print_output:
                    print("\n\nInterview is finished because every candidate is confirmed!\n\n")
                break

            for counterExample in counterExamples:
                if topdown:
                    if Interview.isSuperkeyOfSomeKeys(minKeysFound, counterExample):  # covered in this batch
                        continue
                    minKey = shrink(counterExample)
                    minKeysFound.add(minKey)
                    allMinedKeys.add(minKey)
                    if print_output:
                        print(f"New minimal key: {minKey}")
                else:
                    if Interview.isSubsetOfAnySupersets(maxAntiKeys, counterExample):  # covered in this batch
                        continue
                    antiKey = advance(counterExample)
                    maxAntiKeys.add(antiKey)
                    allNonKeys.add(antiKey)
                    if print_output:
                        print(f"New maximal anti-key: {antiKey}")

        allMinedKeys = Interview.refineToMinimalKeys(allMinedKeys)  # refine interviewed keys to minimal
        return [allMinedKeys, counters[0], counters[1], round_num]

    @staticmethod
    def interview_dualize_with_probability(
            R: List[str],
            p: float,
            print_output: bool,
            rng: Optional[Random] = None,
            traversalStart: str = "bottomup",
            traversalDirection: str = "dfs",
    ) -> List[object]:
        rng = rng or Random()
        return Interview._interview_dualize_core(
            R,
            lambda candKey: rng.random() < p,
            print_output,
            [],
            traversalStart,
            traversalDirection,
        )

    @staticmethod
    def interview_dualize_with_given_minimal_keys(
            R: List[str],
            print_output: bool,
            givenMinimalKeys: List[Key],
            traversalStart: str = "bottomup",
            traversalDirection: str = "dfs",
    ) -> List[object]:
        return Interview._interview_dualize_core(
            R,
            lambda candKey: Interview.isSuperKey(candKey, minimalKeys=givenMinimalKeys),
            print_output,
            [],
            traversalStart,
            traversalDirection,
        )

    @staticmethod
    def interview_dualize_with_predetermined_keys(
            R: List[str],
            print_output: bool,
            givenMinimalKeys: List[Key],
            preDeterminedKeys: List[Key],
            traversalStart: str = "bottomup",
            traversalDirection: str = "dfs",
    ) -> List[object]:
        return Interview._interview_dualize_core(
            R,
            lambda candKey: Interview.isSuperKey(candKey, minimalKeys=givenMinimalKeys),
            print_output,
            preDeterminedKeys,
            traversalStart,
            traversalDirection,
        )

    @staticmethod
    def interview_dualize_with_llm_oracle(
            R: List[str],
            schema_name: str,
            print_output: bool,
            oracle: KeyOracle,
            preDeterminedKeys: Optional[List[Key]] = None,
            traversalStart: str = "bottomup",
            traversalDirection: str = "dfs",
    ) -> List[object]:
        """
        LLM oracle mode of the Dualize and Advance strategy family.
        Returns [minimal keys, number of questions], mirroring interview_with_llm_oracle.
        """
        preDeterminedKeys = preDeterminedKeys or []
        res = Interview._interview_dualize_core(
            R,
            lambda candKey: Interview.isSuperKey(
                candKey,
                oracle=oracle,
                schema=R,
                schema_name=schema_name,
                print_output=print_output,
            ),
            print_output,
            preDeterminedKeys,
            traversalStart,
            traversalDirection,
        )
        return [res[0], res[2]]

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
    import argparse
    import csv
    import os

    # family x starting point x direction -> the eight strategies of the suite
    STRATEGY_SPECS = {
        "LW-TD": ("levelwise", "topdown", "dfs"),
        "LW-TB": ("levelwise", "topdown", "bfs"),
        "LW-BD": ("levelwise", "bottomup", "dfs"),
        "LW-BB": ("levelwise", "bottomup", "bfs"),
        "DA-TD": ("dualize", "topdown", "dfs"),
        "DA-TB": ("dualize", "topdown", "bfs"),
        "DA-BD": ("dualize", "bottomup", "dfs"),
        "DA-BB": ("dualize", "bottomup", "bfs"),
    }
    DA_FAMILY = ["DA-TD", "DA-TB", "DA-BD", "DA-BB"]

    # the Hockey tables of Tab. 5 (full schemata) and Tab. 6 (prime filtering)
    TAB5_SCHEMAS = ["scoring_shootout", "goalies_shootout", "team_vs_team", "combined_shutouts"]
    TAB6_SCHEMAS = TAB5_SCHEMAS + ["teams_half", "teams_post", "goalies", "team_splits"]

    CSV_FIELDS = [
        "schema", "model", "mode", "T", "P",
        "prime_f1", "prime_recall", "prime_precision",
        "strategy",
        "exact_f1", "exact_recall", "exact_precision",
        "relaxed_f1", "relaxed_recall", "relaxed_precision",
        "questions", "minutes", "keys",
    ]

    ap = argparse.ArgumentParser(
        description="LLM-oracle key interviews over the eight strategies of the suite.")
    ap.add_argument("--model", default="deepseek-ai/DeepSeek-R1-Distill-Llama-70B")
    ap.add_argument("--mode", choices=["full", "prime"], required=True,
                    help="full = Tab. 5 (original schema); prime = Tab. 6 (prime filtering)")
    ap.add_argument("--schemas", nargs="+", default=None)
    ap.add_argument("--strategies", nargs="+", default=DA_FAMILY, choices=list(STRATEGY_SPECS))
    ap.add_argument("--outdir", default="results_da")
    ap.add_argument("--max-new-tokens", type=int, default=10000)
    ap.add_argument("--device-map", default="auto")
    ap.add_argument("--no-resume", action="store_true",
                    help="recompute rows that the CSV already holds")
    ap.add_argument("--quiet", action="store_true", help="suppress the per-question transcript")
    args = ap.parse_args()

    R_reduction = (args.mode == "prime")
    schemas = args.schemas or (TAB6_SCHEMAS if R_reduction else TAB5_SCHEMAS)
    model_short_name = args.model.split("/")[-1]
    os.makedirs(args.outdir, exist_ok=True)
    csv_path = os.path.join(args.outdir, args.mode + "_" + model_short_name + ".csv")
    print_output = not args.quiet

    # ---- resume: remember which (schema, strategy) rows are already on disk ----
    done = set()
    if os.path.exists(csv_path) and not args.no_resume:
        with open(csv_path, newline="", encoding="utf-8") as fh:
            for row in csv.DictReader(fh):
                done.add((row["schema"], row["strategy"]))
        print("[resume] " + csv_path + " already holds " + str(len(done)) + " rows")
    if not os.path.exists(csv_path):
        with open(csv_path, "w", newline="", encoding="utf-8") as fh:
            csv.DictWriter(fh, fieldnames=CSV_FIELDS).writeheader()

    def emit(row):
        with open(csv_path, "a", newline="", encoding="utf-8") as fh:
            csv.DictWriter(fh, fieldnames=CSV_FIELDS).writerow(row)

    def keys_to_text(keys):
        return " | ".join(sorted(",".join(sorted(k.getAttributes())) for k in keys))

    def keys_from_text(text):
        out = []
        for part in (text or "").split(" | "):
            part = part.strip()
            if part:
                out.append(Key([a for a in part.split(",") if a]))
        return out

    def write_aggregates():
        """Derive Agg. and Mk from the recorded per-strategy rows.

        The aggregates are a function of the strategy rows, never of what this
        particular process happened to run: a resumed run skips the strategies
        the CSV already holds, so accumulating them in the loop would silently
        aggregate over fewer than the four members of the family. This pass
        rewrites the aggregate file from scratch on every run, which also keeps
        a resumed run from appending a second, stale set of aggregate rows.
        """
        if not os.path.exists(csv_path):
            return
        by_schema = {}
        with open(csv_path, newline="", encoding="utf-8") as fh:
            for row in csv.DictReader(fh):
                if row["strategy"] in STRATEGY_SPECS:  # strategy rows only
                    by_schema.setdefault(row["schema"], {})[row["strategy"]] = row
        agg_path = os.path.join(
            args.outdir, "agg_" + args.mode + "_" + model_short_name + ".csv")
        fields = CSV_FIELDS + ["members"]
        with open(agg_path, "w", newline="", encoding="utf-8") as fh:
            writer = csv.DictWriter(fh, fieldnames=fields)
            writer.writeheader()
            for schema_name in schemas:
                rows = by_schema.get(schema_name)
                if not rows:
                    continue
                _, gt = schema_info_db(schema_name)
                freq, tq, tm = {}, 0, 0.0
                for strategy in sorted(rows):
                    row = rows[strategy]
                    for k in keys_from_text(row["keys"]):
                        freq[k] = freq.get(k, 0) + 1
                    tq += int(row["questions"] or 0)
                    tm += float(row["minutes"] or 0.0)
                any_row = rows[sorted(rows)[0]]
                base = {f: any_row.get(f, "") for f in
                        ("schema", "model", "mode", "T", "P",
                         "prime_f1", "prime_recall", "prime_precision")}
                base["members"] = len(rows)
                if not freq:
                    continue
                ks = set(freq)
                p, r, f1 = evaluate_keys(gt, ks)
                pr, rr, f1r = evaluate_keys_relaxed(gt, ks)
                writer.writerow(dict(base, strategy="Agg.",
                                     exact_f1=f"{f1:.4f}", exact_recall=f"{r:.4f}",
                                     exact_precision=f"{p:.4f}",
                                     relaxed_f1=f"{f1r:.4f}", relaxed_recall=f"{rr:.4f}",
                                     relaxed_precision=f"{pr:.4f}",
                                     questions=tq, minutes=f"{tm:.4f}",
                                     keys=keys_to_text(ks)))
                for k_freq, min_keys in compute_topk_freq_minimal_keys(freq):
                    p, r, f1 = evaluate_keys(gt, min_keys)
                    pr, rr, f1r = evaluate_keys_relaxed(gt, min_keys)
                    writer.writerow(dict(base, strategy="M" + str(k_freq),
                                         exact_f1=f"{f1:.4f}", exact_recall=f"{r:.4f}",
                                         exact_precision=f"{p:.4f}",
                                         relaxed_f1=f"{f1r:.4f}", relaxed_recall=f"{rr:.4f}",
                                         relaxed_precision=f"{pr:.4f}",
                                         questions="", minutes="",
                                         keys=keys_to_text(min_keys)))
                print(f"[agg] {schema_name}: {len(rows)} strategies, "
                      f"{tq} questions, {tm:.2f} min")
        print("Aggregates rewritten in " + agg_path)

    oracle = KeyInterviewOracle(
        model_name_or_path=args.model,
        device_map=args.device_map,
        max_new_tokens=args.max_new_tokens,
    )
    print("Using LLM Model: " + args.model + " | mode: " + args.mode
          + " | strategies: " + ",".join(args.strategies))

    for schema_name in schemas:
        R_full, groundTruthMinimalKeys = schema_info_db(schema_name)
        R = list(R_full)
        print("\n" + "#" * 70)
        print(f"Schema Name: {schema_name} | Schema: {R} "
              f"| Ground Truth Minimal Keys: {groundTruthMinimalKeys}")
        res_path = os.path.join(
            args.outdir, "result_" + args.mode + "_" + model_short_name
            + "_" + schema_name + ".txt")
        with open(res_path, "a", encoding="utf-8") as f:
            f.write("#" * 20 + "\n")
            f.write(f"Schema Name: {schema_name} | Schema: {R} "
                    f"| Ground Truth Minimal Keys: {groundTruthMinimalKeys}\n"
                    f"Using LLM Model: {args.model} | mode: {args.mode}\n")

        # ---- prime filtering: predict once per (schema, model) and pin it on disk,
        # ---- so that every strategy and every resumed run interviews the same schema
        prime_stats = {"P": "", "prime_f1": "", "prime_recall": "", "prime_precision": ""}
        if R_reduction:
            pin_path = os.path.join(
                args.outdir, "prime_" + model_short_name + "_" + schema_name + ".json")
            if os.path.exists(pin_path):
                with open(pin_path, encoding="utf-8") as fh:
                    pinned = json.load(fh)
                predicted_prime_attrs = pinned["predicted_prime_attrs"]
                print(f"[pinned] prime attributes from {pin_path}: {predicted_prime_attrs}")
            else:
                # ask_prime_attributes raises when the answer does not parse, and
                # greedy decoding would return the same answer on every retry, so a
                # failure here is skipped rather than retried: the remaining schemas
                # still run, and the CSV records which one was lost.
                try:
                    predicted_prime_attrs = oracle.ask_prime_attributes(
                        schema=R, schema_name=schema_name, print_output=print_output)
                except Exception as exc:
                    print(f"[FAILED] prime attributes for {schema_name}: {exc!r}")
                    emit(dict(schema=schema_name, model=model_short_name, mode=args.mode,
                              T=len(R_full), P="", prime_f1="", prime_recall="",
                              prime_precision="", strategy="PRIME-FAILED",
                              exact_f1="", exact_recall="", exact_precision="",
                              relaxed_f1="", relaxed_recall="", relaxed_precision="",
                              questions="", minutes="", keys=repr(exc)[:300]))
                    continue
                with open(pin_path, "w", encoding="utf-8") as fh:
                    json.dump({"predicted_prime_attrs": list(predicted_prime_attrs)}, fh)
            ground_truth_prime_attrs = compute_ground_truth_prime_attributes(
                groundTruthMinimalKeys)
            p_p, p_r, p_f1 = evaluate_prime_attributes(
                ground_truth_prime_attrs, predicted_prime_attrs)
            R_prime = [a for a in R if a in predicted_prime_attrs]
            prime_res = (f"Ground truth prime attrs: {ground_truth_prime_attrs}\n"
                         f"Predicted prime attrs: {predicted_prime_attrs}\n"
                         f"Prime Attr F1, recall, precision: "
                         f"{p_f1:.4f}, {p_r:.4f}, {p_p:.4f}\n"
                         f"Reduced schema after prime pruning: {R_prime}\n"
                         "=============================\n")
            print(prime_res)
            with open(res_path, "a", encoding="utf-8") as f:
                f.write(prime_res + "\n")
            prime_stats = {"P": len(R_prime), "prime_f1": f"{p_f1:.4f}",
                           "prime_recall": f"{p_r:.4f}", "prime_precision": f"{p_p:.4f}"}
            R = R_prime

        base_row = dict(schema=schema_name, model=model_short_name, mode=args.mode,
                        T=len(R_full), **prime_stats)

        all_mined_min_keys_dict = {}
        total_cost_min = 0.0
        total_questions = 0
        for strategy in args.strategies:
            family, traversalStart, traversalDirection = STRATEGY_SPECS[strategy]
            if (schema_name, strategy) in done:
                print(f"[skip] {schema_name} {strategy} already recorded")
                continue

            print(f"\n===== {schema_name} | {strategy} "
                  f"({family}, {traversalStart}, {traversalDirection}) =====")
            start_time = time.perf_counter()
            # one strategy that fails must not cost the whole unattended run
            try:
                if family == "dualize":
                    result = Interview.interview_dualize_with_llm_oracle(
                        R=R, schema_name=schema_name, print_output=print_output,
                        oracle=oracle, preDeterminedKeys=[], traversalStart=traversalStart,
                        traversalDirection=traversalDirection)
                else:
                    result = Interview.interview_with_llm_oracle(
                        traversalStart=traversalStart, traversalDirection=traversalDirection,
                        R=R, schema_name=schema_name, print_output=print_output,
                        oracle=oracle, preDeterminedKeys=[])
            except Exception as exc:
                import traceback
                traceback.print_exc()
                print(f"[FAILED] {schema_name} {strategy}: {exc!r}")
                emit(dict(base_row, strategy=strategy + "-FAILED",
                          exact_f1="", exact_recall="", exact_precision="",
                          relaxed_f1="", relaxed_recall="", relaxed_precision="",
                          questions="", minutes=f"{(time.perf_counter() - start_time) / 60.0:.4f}",
                          keys=repr(exc)[:300]))
                continue
            elapsed_min = (time.perf_counter() - start_time) / 60.0
            total_cost_min += elapsed_min

            interviewedMinimalKeys, num_all_questions = result[0], result[1]
            total_questions += num_all_questions
            for k in interviewedMinimalKeys:
                all_mined_min_keys_dict[k] = all_mined_min_keys_dict.get(k, 0) + 1

            precision, recall, f1 = evaluate_keys(
                groundTruthMinimalKeys, interviewedMinimalKeys)
            precision_rel, recall_rel, f1_rel = evaluate_keys_relaxed(
                groundTruthMinimalKeys, interviewedMinimalKeys)

            block = ("\n======= Evaluation =======\n"
                     f"{strategy} ({family}, {traversalStart}, {traversalDirection})\n"
                     f"Time cost: {elapsed_min:.4f} minutes\n"
                     f"Mined minimal keys: {interviewedMinimalKeys} "
                     f"| # Questions: {num_all_questions}\n"
                     f"F1, recall, precision: {f1:.4f}, {recall:.4f}, {precision:.4f}\n"
                     f"Relaxed F1, recall, precision: "
                     f"{f1_rel:.4f}, {recall_rel:.4f}, {precision_rel:.4f}\n"
                     "==========================\n\n")
            print(block)
            with open(res_path, "a", encoding="utf-8") as f:
                f.write(block)

            emit(dict(base_row, strategy=strategy,
                      exact_f1=f"{f1:.4f}", exact_recall=f"{recall:.4f}",
                      exact_precision=f"{precision:.4f}",
                      relaxed_f1=f"{f1_rel:.4f}", relaxed_recall=f"{recall_rel:.4f}",
                      relaxed_precision=f"{precision_rel:.4f}",
                      questions=num_all_questions, minutes=f"{elapsed_min:.4f}",
                      keys=keys_to_text(interviewedMinimalKeys)))

        if not all_mined_min_keys_dict:
            print(f"[note] {schema_name}: every strategy was already recorded")
        else:
            print(f"[run] {schema_name}: {total_questions} questions, "
                  f"{total_cost_min:.2f} min in this process")

    # Agg. and Mk are a function of the recorded strategy rows, so they are
    # derived once here rather than accumulated while the schemas run.
    write_aggregates()
    print("\nAll done. Strategy rows are in " + csv_path)
